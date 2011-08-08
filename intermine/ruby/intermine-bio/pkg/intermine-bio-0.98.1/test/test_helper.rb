$LOAD_PATH << File.expand_path( File.dirname(__FILE__) + '/../lib' )
require "rexml/document"
require "test/unit"

include Test::Unit::Assertions

def compare_xml(a, b)
    require "rexml/document"
    docA = REXML::Document.new(a.to_s)
    docB = REXML::Document.new(b.to_s)

    a_elems = docA.elements.to_a
    b_elems = docB.elements.to_a

    (0 ... a_elems.size).each do |idx|
        compare_elements(a_elems[idx], b_elems[idx])
    end
end

private

def fail_xml_compare(elemA, elemB, problem, e)
    formatter = REXML::Formatters::Pretty.new
    elemA_str = String.new
    elemB_str = String.new
    formatter.write(elemA, elemA_str)
    formatter.write(elemB, elemB_str)
    first_part = "#{elemA_str}\nis not equal to\n#{elemB_str}\n"
    
    raise Test::Unit::AssertionFailedError, "#{first_part}because #{problem} - #{e.message}" 
end

def compare_elements(elemA, elemB)

    begin
        assert_equal(elemA.name, elemB.name)
    rescue Test::Unit::AssertionFailedError => e
        fail_xml_compare(elemA, elemB, "names of element differ", e)
    end

    begin
        assert_equal(elemA.attributes, elemB.attributes)
    rescue Test::Unit::AssertionFailedError => e
        fail_xml_compare(elemA, elemB, "attributes of element differ", e)
    end

    begin
        assert_equal(elemA.text, elemB.text)
    rescue Test::Unit::AssertionFailedError => e
        fail_xml_compare(elemA, elemB, "text contents of element differ", e)
    end

    begin
        assert_equal(elemA.elements.size, elemB.elements.size)
    rescue Test::Unit::AssertionFailedError => e
        fail_xml_compare(elemA, elemB, "number of children of element differ", e)
    end

    a_elems = elemA.elements.to_a
    b_elems = elemB.elements.to_a

    (0 ... a_elems.size).each do |idx|
        compare_elements(a_elems[idx], b_elems[idx])
    end
end


