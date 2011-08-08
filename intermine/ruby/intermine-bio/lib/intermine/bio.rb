require "net/http"

# == Biologically specific Extensions to the InterMine Data-Warehousing Webservices
#
# These modules provide interfaces to the biologically specific elements of the 
# InterMine webservices. At present this consists of the ability to return results 
# from queries in Biologically specific formats (GFF3, UCSC-BED, FASTA). The 
# methods for accessing these formats provide mechanisms for iterating over the contents
# in logical chunks.
#
#:include:contact_header.rdoc
#
module InterMine

    # The Metadata for these extensions
    #
    #:include:contact_header.rdoc
    #
    module Bio

        # The library version
        VERSION = "0.98.1"
        
        # The library name
        NAME = "intermine-bio"

        # The project's homepage
        HOMEPAGE = "http://www.intermine.org"

        # The authors of this library
        AUTHORS = ["Alex Kalderimis"]

        # An email address to seek support at
        EMAIL = ["dev@intermine.org"]
    end

    # Extensions to the PathQuery module.
    #
    #:include:contact_header.rdoc
    #
    module PathQuery

        class BioError < RuntimeError
        end

        # Biologically specific Extensions to the Query class. 
        #
        # These methods provide mechanisms for accessing results in Biologically
        # appropriate formats, being at present GFF3, FASTA, and UCSC-BED.
        #
        # The methods provided here can be used to both return the data as a string,
        # and to iterate over the data in logical chunks, approriate to the format in question.
        #
        #:include:contact_header.rdoc
        #
        class Query

            # Return the results from this query as GFF3
            #
            # If a block is given, each line of the GFF3 will be 
            # yielded in turn, omitting any header lines, which are 
            # returned at the end of the iteration, otherwise
            # the content of the GFF3 results will be returned as one string.
            #
            #   header = query.gff3 do |line|
            #      process line
            #   end
            #
            #   puts query.gff3
            #
            def gff3
                if block_given?
                    header = ""
                    results_reader.each_gff3 do |gff3|
                        if gff3.start_with? "#"
                            header << gff3
                        else
                            yield gff3
                        end
                    end
                    return header
                else
                    buffer = ""
                    results_reader.each_gff3 do |gff3|
                        buffer.concat(gff3)
                    end
                    return buffer
                end
            end

            # Return the results from this query as FASTA
            #
            # If a block is given, each FASTA record will be yielded
            # in turn, and the query will be returned, otherwise
            # the content of the FASTA results will be returned as one string.
            #
            #   query.fasta do |record|
            #      process record
            #   end
            #
            #   puts query.fasta
            #
            def fasta # :yields: record
                if block_given?
                    buffer = nil
                    results_reader.each_fasta do |line|
                        if line.start_with? ">"
                            yield buffer unless buffer.nil?
                            buffer = line
                        else
                            raise BioError, "Incorrect fasta - no header line" if buffer.nil?
                            buffer << line
                        end
                    end
                    yield buffer unless buffer.nil?
                    return self
                else
                    buffer = ""
                    results_reader.each_fasta do |line|
                        buffer.concat(line)
                    end
                    return buffer
                end
            end

            # Return the results from this query as BED
            #
            # If a block is given, each line of the BED results will be 
            # yielded in turn, and the header will be returned, otherwise
            # the content of the BED results will be returned as one string.
            #
            # If the optional parameter is set to false, then the "chr" prefix on
            # the chromosome id will be omitted.
            #
            #   header = query.bed do |line|
            #      process line
            #   end
            #
            #   puts query.bed(false)
            #
            def bed(ucscCompatible=true)
                if block_given?
                    header = ""
                    results_reader.each_bed(ucscCompatible) do |bed|
                        if bed =~ /^\s*(#|track)/
                            header << bed
                        else
                            yield bed
                        end
                    end
                    return header
                else
                    buffer = ""
                    results_reader.each_bed(ucscCompatible) do |bed|
                        buffer.concat(bed)
                    end
                    return buffer
                end
            end
        end
    end

    # Extensions to the Result processing code
    #
    #:include:contact_header.rdoc
    #
    module Results

        # Extensions to the ResultsReader object
        #
        # These methods provide mechanisms for accessing the results in raw 
        # format, and iterating over them line by line in a memory efficient
        # manner. They are in no way content aware.
        #
        #:include:contact_header.rdoc
        #
        class ResultsReader

            # The path to use to get the resource paths for the query variants
            RESOURCE_PATH = "/check/"

            # Yield results as GFF3
            def each_gff3
                adjust_path(:gff3)
                each_line(params("gff3")) do |line|
                    yield line
                end
            end

            # Yield results as fasta
            def each_fasta
                adjust_path(:fasta)
                each_line(params("fasta")) do |line|
                    yield line
                end
            end

            # Yield results as UCSC-BED
            def each_bed(ucscCompatible=true)
                adjust_path(:bed)
                p = params("bed")
                p["ucscCompatible"] = "no" unless ucscCompatible
                each_line(p) do |line|
                    yield line
                end
            end

            # Adjust the path of this query to suit the currently selected format.
            def adjust_path(variant)
                @resources ||= {}
                root = @query.service.root
                uri = URI.parse(root + RESOURCE_PATH + "query." + variant.to_s)
                @resources[variant] = Net::HTTP.get(uri.host, uri.path)
                path = @resources[variant]
                @uri = URI.parse(root + path)
            end

        end
    end
end

