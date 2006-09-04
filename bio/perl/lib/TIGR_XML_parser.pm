#!/usr/local/bin/perl

package TIGR_XML_parser;

use strict;
use Gene_obj;
use XML::Simple;
use Data::Dumper;
use Evidence;
use Gene_ontology;

sub new {
    my $self = {gene_objs=>[],
	    assembly_seq => 0,
	    clone_name=>0,
	    asmbl_id=>0,
	    chromosome=>0,
	    clone_id=>0,
	    gb_acc=>0,
	    seq_group=>0,
	    gene_evidence_hashref=>{},
	    model_evidence_hashref=>{}
	    };
    bless $self;
    return $self;
}


sub capture_genes_from_assembly_xml {
    my $self = shift;
    my $xml_file = shift;
    print STDERR "Parsing xml file: $xml_file\n";
    my $ref = XMLin($xml_file, forcearray=>1, keeproot=>1, forcecontent=>1, keyattr=>{});
    #print Dumper($ref);

    my $asmbl_ref;
    if (exists ($ref->{TIGR}->[0]->{ASSEMBLY})) {
	$asmbl_ref = $ref->{TIGR}->[0]->{ASSEMBLY};
    } elsif (exists ($ref->{TIGR}->[0]->{PSEUDOCHROMOSOME}->[0]->{ASSEMBLY})) {
	$asmbl_ref = $ref->{TIGR}->[0]->{PSEUDOCHROMOSOME}->[0]->{ASSEMBLY};
    } else {
	die "No assemblies to parse\n";
    }
    


    $self->{clone_name} = &content($asmbl_ref->[0]->{HEADER}->[0]->{CLONE_NAME});
    $self->{asmbl_id} = &content($asmbl_ref->[0]->{ASMBL_ID});
    $self->{chromosome} =  $asmbl_ref->[0]->{CHROMOSOME};
    $self->{clone_id} = $asmbl_ref->[0]->{CLONE_ID};
    $self->{gb_acc} = &content($asmbl_ref->[0]->{HEADER}->[0]->{GB_ACCESSION});
    $self->{seq_group} = &content($asmbl_ref->[0]->{HEADER}->[0]->{SEQ_GROUP});
 

    ## Process the Protein Coding Genes
    my $genelist_arrayref = $asmbl_ref->[0]->{GENE_LIST}->[0]->{PROTEIN_CODING}->[0]->{TU};
    my $x = 1;
    print STDERR "Creating Gene objects\n";
    foreach my $gene_ref_xml (@$genelist_arrayref) {
	my $gene_obj = &process_gene ($self, $gene_ref_xml);
	$self->add_gene_obj($gene_obj);
	$x++;
    }
    
    ## Process the RNA Genes.
    my $RNAgenelist_hashref = $asmbl_ref->[0]->{GENE_LIST}->[0]->{RNA_GENES}->[0];
    foreach my $rna_gene_type (keys %$RNAgenelist_hashref) {
	my $list_of_rna_genes = $RNAgenelist_hashref->{$rna_gene_type};
	foreach my $gene_ref_xml (@$list_of_rna_genes) {
	    my $gene_obj = &process_rna_gene($rna_gene_type, $gene_ref_xml);
	    $gene_obj->refine_gene_object();
	    $self->add_gene_obj($gene_obj);
	    $x++;
	}
    }
    
    # get assembly sequence.
    $self->{assembly_seq} = &content($asmbl_ref->[0]->{ASSEMBLY_SEQUENCE});
    $self->{assembly_seq} =~ s/\s+//g; #rid whitespace if any.
}

####
sub process_gene {
    my ($self, $gene_ref_xml) = @_;
    my $gene_obj = new Gene_obj();
    $gene_obj->{TU_feat_name} = &content($gene_ref_xml->{FEAT_NAME});
    &process_gene_info ($self, $gene_obj, $gene_ref_xml->{GENE_INFO}->[0]); #send hash ref
    &process_evidence($self, $self->{gene_evidence_hashref}, $gene_obj->{TU_feat_name}, $gene_ref_xml->{GENE_EVIDENCE}->[0]);
    &process_gene_models ($self, $gene_obj, $gene_ref_xml->{MODEL}); #send array ref
    
    return ($gene_obj);
}

####
sub process_gene_info {
    my ($self, $gene_obj, $gene_info_ref) = @_;
    $gene_obj->{locus} =  &content($gene_info_ref->{LOCUS});
    $gene_obj->{pub_locus} =  &content($gene_info_ref->{PUB_LOCUS});
    $gene_obj->{alt_locus} = &content($gene_info_ref->{ALT_LOCUS});
    
    my ($com_name, $secondary_product_names_aref) = &process_function_values ($gene_info_ref->{COM_NAME});
    if ($com_name) {
	$gene_obj->{com_name} = $com_name;
    }
    if (@$secondary_product_names_aref) {
	$gene_obj->add_secondary_product_names(@$secondary_product_names_aref);
    }
    
    my ($gene_name, $secondary_gene_names_aref) = &process_function_values ($gene_info_ref->{GENE_NAME});
    if ($gene_name) {
	$gene_obj->{gene_name} = $gene_name;
    }
    if (@$secondary_gene_names_aref) {
	$gene_obj->add_secondary_gene_names(@$secondary_gene_names_aref);
    }

    my ($ec_number, $secondary_ec_numbers_aref) = &process_function_values ($gene_info_ref->{EC_NUM});
    if ($ec_number) {
	$gene_obj->{ec_num} = $ec_number;
    }
    if (@$secondary_ec_numbers_aref) {
	$gene_obj->add_secondary_ec_numbers(@$secondary_ec_numbers_aref);
    }

    my ($gene_symbol, $secondary_gene_symbols_aref) = &process_function_values ($gene_info_ref->{GENE_SYM});
    if ($gene_symbol) {
	$gene_obj->{gene_sym} = $gene_symbol;
    }
    if (@$secondary_gene_symbols_aref) {
	$gene_obj->add_secondary_gene_symbols(@$secondary_gene_symbols_aref);
    }
    
    $gene_obj->{pub_comment} = &content($gene_info_ref->{PUB_COMMENT});
    $gene_obj->{is_pseudogene} =  &content($gene_info_ref->{IS_PSEUDOGENE});
    
    if (my $gene_ontology_xml_aref = $gene_info_ref->{GENE_ONTOLOGY}) {
	&process_gene_ontology($self, $gene_obj, $gene_ontology_xml_aref->[0]);
    }
}


sub process_function_values {
    my ($xml_ref) = @_;
    my ($primary_value, @secondary_values);
    
    unless (ref $xml_ref eq "ARRAY") {
	return ("", \@secondary_values);
    }
   
    my @elements = @$xml_ref;
    foreach my $element (@elements) {
	my $is_primary = $element->{IS_PRIMARY};
	my $value = $element->{content};
	if ($is_primary) {
	    $primary_value = $value;
	} else {
	    push (@secondary_values, $value);
	}
    }
    return ($primary_value, \@secondary_values);
}


####
sub process_rna_gene {
    my ($rna_gene_type, $gene_ref_xml) = @_;
    if ($rna_gene_type =~ /pre\-trna/i) {
	$rna_gene_type = "tRNA";
    }
    my $gene_obj = new Gene_obj();
    #print Dumper ($gene_ref_xml);
    $gene_obj->{pub_locus} = &content($gene_ref_xml->{PUB_LOCUS});
    $gene_obj->{com_name} = &content($gene_ref_xml->{COM_NAME});
    $gene_obj->{gene_type} = lc ($rna_gene_type);
    $gene_obj->{TU_feat_name} = &content($gene_ref_xml->{FEAT_NAME});
    &get_exon_info($gene_obj, $gene_ref_xml);
    return ($gene_obj);
}


####
sub content {
    my ($arrayref) = @_;
    if (ref $arrayref eq "ARRAY") {
	my $content = $arrayref->[0]->{content};
	$content =~ s/^\s+|\s+$//g; #trim leading and trailing whitespace chars.
	return ($content);
    } else {
	return ("");
    }
}

####
sub process_gene_models {
    my ($self, $gene_obj, $gene_models_ref) = @_;
    
    my $new_gene_obj;

    foreach my $gene_model_ref (@$gene_models_ref) {
	if ($new_gene_obj) {
	    $new_gene_obj = new Gene_obj();
	    ## Copy over functional annotations:
	    $new_gene_obj->{TU_feat_name} = $gene_obj->{TU_feat_name};
	    $new_gene_obj->{com_name} = $gene_obj->{com_name};
	    $new_gene_obj->{pub_locus} = $gene_obj->{pub_locus};
	    $new_gene_obj->{locus} = $gene_obj->{locus};
	    $new_gene_obj->{pub_comment} = $gene_obj->{pub_comment};
	    
	    $gene_obj->add_isoform($new_gene_obj);
	} else {
	    $new_gene_obj = $gene_obj;
	}
	my $Model_feat_name = &content($gene_model_ref->{FEAT_NAME});
	$new_gene_obj->{Model_feat_name} = $Model_feat_name;
	my $model_pub_locus = &content($gene_model_ref->{PUB_LOCUS});
	$new_gene_obj->{model_pub_locus} = $model_pub_locus;
	
	my @exon_refs = @{$gene_model_ref->{EXON}};
	foreach my $exon_ref (@exon_refs) {
	    &get_exon_info($new_gene_obj, $exon_ref);
	}
	my $model_evidence_xmlref = $gene_model_ref->{MODEL_EVIDENCE};
	if ($model_evidence_xmlref) {
	    &process_evidence($self, $self->{model_evidence_hashref}, $Model_feat_name, $model_evidence_xmlref->[0]);
	}
    }
    $gene_obj->refine_gene_object();
}


####
sub get_exon_info {
    my ($gene_obj, $exon_ref) = @_;
    my $exon_end5 = &content($exon_ref->{COORDSET}->[0]->{END5});
    my $exon_end3 = &content($exon_ref->{COORDSET}->[0]->{END3});
    my $exon_obj = new mRNA_exon_obj($exon_end5, $exon_end3);
    if (exists ($exon_ref->{CDS})) {
	my ($cds_end5, $cds_end3) = &get_cds_coords ($exon_ref->{CDS}->[0]);
	$exon_obj->add_CDS_exon_obj($cds_end5, $cds_end3);
    }
    $gene_obj->add_mRNA_exon_obj($exon_obj);
}


####
sub get_cds_coords {
    my ($cds_ref) = @_;
    my $end5 = &content($cds_ref->{COORDSET}->[0]->{END5});
    my $end3 = &content($cds_ref->{COORDSET}->[0]->{END3});
    return ($end5, $end3);
}




sub add_gene_obj {
    my $self = shift;
    my $gene_obj = shift;
    my $index = $#{$self->{gene_objs}};
    $index++;
    $self->{gene_objs}->[$index] = $gene_obj;
}

sub get_genes {
    my $self = shift;
    my $gene_type = shift;
    my @genes = @{$self->{gene_objs}};
    if ($gene_type) {
	my @genes_of_type;
	foreach my $gene (@genes) {
	    if ($gene->{gene_type} eq $gene_type) {
		push (@genes_of_type, $gene);
	    }
	}
	return (@genes_of_type);
    } else {
	return (@genes);
    }
}



sub get_assembly_sequence {
    my $self = shift;
    return ($self->{assembly_seq});
}


# if no CDSs are specified, want to create CDSs based on mRNA exons.
sub force_CDS_occupancy {
    my $self = shift;
    my @genes = $self->get_genes();
    foreach my $gene (@genes) {
	my @exons = $gene->get_exons();
	foreach my $exon (@exons) {
	    my ($end5, $end3) = $exon->get_mRNA_exon_end5_end3();
	    $exon->add_CDS_exon_obj($end5, $end3);
	}
	$gene->refine_gene_object();
    }
}




sub toString {
    my $self = shift;
    
    ## Dump header
    my $text = "Clone_name: $self->{clone_name}\n"
	. "Clone_id: $self->{clone_id}\n"
	    . "Chromosome: $self->{chromosome}\n"
	. "Seq_group: $self->{seq_group}\n"
	. "Genbank Accession: $self->{gb_acc}\n"
	    . "TIGR Asmbl_id: $self->{asmbl_id}\n";
    
    ## Dump genes
    $text .= "\n\nGenes:\n";
    my @genes = $self->get_genes();
    my $x = 1;
    foreach my $gene (@genes) {
	$text .= "gene: $x\n" . $gene->toString();
	$x++;
    }
    
    ## Dump Gene evidence:
    $text .= "\n\nGene Evidence:\n";
    my $gene_evidence_href = $self->{gene_evidence_hashref};
    foreach my $feat_name (keys %$gene_evidence_href) {
	my $list_ref = $gene_evidence_href->{$feat_name};
	foreach my $evidence (@$list_ref) {
	    $text .= $evidence->toString();
	}
    }

    ## Dump Model evidence:
    $text .= "\n\nModel evidence:\n";
    my $model_evidence_hashref = $self->{model_evidence_hashref};
    foreach my $feat_name (keys %$model_evidence_hashref) {
	my $listref = $model_evidence_hashref->{$feat_name};
	foreach my $evidence (@$listref) {
	    $text .= $evidence->toString();
	}
    }
    

    #$text .= "\n\nAssembly Sequence:\n" . $self->get_assembly_sequence();
    return ($text);
}


####
sub process_evidence {
    my ($self, $container, $feat_name, $evidence_xml_ref) = @_;
    ## Process sequence db matches:
    my $sequence_db_matchesref = $evidence_xml_ref->{EVIDENCE_TYPE}->[0]->{SEQUENCE_DB_MATCH}->[0];
    my $search_dbs = $sequence_db_matchesref->{SEARCH_DB};
    foreach my $search_db (@$search_dbs) {
	my $search_db_name = $search_db->{DB_NAME};
	my $seq_elements_aref = $search_db->{SEQ_ELEMENT};
	&create_evidence_from_seq_elements($self, $container, $feat_name, "SEQUENCE_DB_MATCH", $search_db_name, $seq_elements_aref);
    }
    
    ## process comput predictions.
    my $comput_preds = $evidence_xml_ref->{EVIDENCE_TYPE}->[0]->{COMPUT_PREDICTION}->[0];
    my $prediction_sets = $comput_preds->{PREDICTION_SET};
    foreach my $prediction (@$prediction_sets) {
	my $pred_tool = $prediction->{PREDICTION_TOOL};
	my $seq_elements_aref = $prediction->{SEQ_ELEMENT};
	&create_evidence_from_seq_elements($self, $container, $feat_name, "COMPUT_PREDICTION", $pred_tool, $seq_elements_aref);
    }
    
    
}




sub create_evidence_from_seq_elements {
    my ($self, $container, $feat_name, $ev_class, $dbname_or_prediction_tool, $seq_elements_aref) = @_;
    foreach my $seq_element (@$seq_elements_aref) {
	my $evidence = new Evidence();
	$evidence->{FEAT_NAME} = $feat_name;
	$evidence->{EV_CLASS} = $ev_class;
	if ($ev_class eq "SEQUENCE_DB_MATCH") {
	    $evidence->{DB_NAME} = $dbname_or_prediction_tool;
	} elsif ($ev_class eq "COMPUT_PREDICTION") {
	    $evidence->{PREDICTION_TOOL} = $dbname_or_prediction_tool;
	}
	

	foreach my $key (keys %$seq_element) {
	    if (ref ($seq_element->{$key})) { #must be a coordset:
		my $coordset_xmlref = $seq_element->{$key}->[0];
		
		
		my $end5 = &content($coordset_xmlref->{COORDSET}->[0]->{END5});
		my $end3 = &content($coordset_xmlref->{COORDSET}->[0]->{END3});
		
		$evidence->set_evidence_coords($key, $end5, $end3);
	       
	    } else {
		$evidence->{$key} = $seq_element->{$key};
	    }
	}
       
	## add to container:
	my $ev_list = $container->{$feat_name};
	unless (ref $ev_list) {
	    $container->{$feat_name} = $ev_list = [];
	}
	push (@$ev_list, $evidence);
	
    }
}

####
sub process_gene_ontology {
    my ($self, $gene_obj, $gene_ontology_xml_href) = @_;
    foreach my $go_id_xml_href (@{$gene_ontology_xml_href->{GO_ID}}) {
	my $go_id = $go_id_xml_href->{ASSIGNMENT};
	my $go_type = &content($go_id_xml_href->{GO_TYPE});
	my $go_term = &content($go_id_xml_href->{GO_TERM});
	my $date = &content($go_id_xml_href->{DATE});
	my $ontology_obj = new Gene_ontology($go_id, $go_term, $go_type);
	
	my $go_evidence_aref = $go_id_xml_href->{GO_EVIDENCE};
	foreach my $go_evidence_href (@$go_evidence_aref) {
	    my $ev_code = $go_evidence_href->{EV_CODE}->[0]->{CODE};
	    my $evidence = &content($go_evidence_href->{EVIDENCE});
	    my $with_ev;
	    if ($go_evidence_href->{WITH_EV}) {
		$with_ev = &content($go_evidence_href->{WITH_EV});
	    }
	    $ontology_obj->add_evidence($ev_code, $evidence, $with_ev);
	}
	$gene_obj->add_gene_ontology_objs($ontology_obj);
	
    }
       
}




1;# end of TIGR_db_parser



