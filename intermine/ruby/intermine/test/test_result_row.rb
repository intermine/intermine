require File.dirname(__FILE__) + "/test_helper.rb"
require "intermine/results"
require 'rubygems'
require "json"
require "test/unit"

class TestResults < Test::Unit::TestCase

    include Results

    def initialize(name)
        super
        file = File.new(
            File.dirname(__FILE__) + "/data/resultrow.json", "r")
        @data = file.read
        @json = JSON.parse(@data)
        @view = ["Company.departments.name","Company.departments.manager.name", "Company.departments.manager.age"]
        @bad_view = ["Company.departments.name","Company.departments.manager.name"]
    end

    def test_initialize
        rr = ResultsRow.new(@data, @view)
        assert_kind_of(ResultsRow, rr)

        rr = ResultsRow.new(@json, @view)
        assert_kind_of(ResultsRow, rr)

        assert_raise ArgumentError do
            ResultsRow.new
        end

        assert_raise ArgumentError do
            ResultsRow.new(@data)
        end

        assert_raise ArgumentError do
            ResultsRow.new(@data, @bad_view)
        end

        assert_raise ArgumentError do
            ResultsRow.new(@json, @bad_view)
        end
    end

    def test_array_access

        rr = ResultsRow.new(@data, @view)

        assert_equal(rr[0], "DepartmentA1")
        assert_equal(rr[1], "EmployeeA1")

        rr = ResultsRow.new(@json, @view)

        assert_equal(rr[0], "DepartmentA1")
        assert_equal(rr[1], "EmployeeA1")

        assert_equal(rr[1..2], ["EmployeeA1", 33])
        assert_equal(rr[0, 2], ["DepartmentA1", "EmployeeA1"])

        assert_equal(rr.first, "DepartmentA1")
        assert_equal(rr.last, 33)

        assert_raise IndexError do
            rr[3]
        end

    end

    def test_each

        rr = ResultsRow.new(@data, @view)
        expected =  %w{DepartmentA1 EmployeeA1} << 33

        c = 0
        rr.each do |val|
            assert_equal(expected[c], val)
            c += 1
        end

        c = 0
        rr.each do |key, val|
            assert_equal(@view[c], key)
            assert_equal(expected[c], val)
            c += 1
        end
    end

    def test_hash_access

        rr = ResultsRow.new(@data, @view)

        assert_equal(rr["Company.departments.name"], "DepartmentA1")
        assert_equal(rr["Company.departments.manager.name"], "EmployeeA1")

        rr = ResultsRow.new(@json, @view)

        assert_equal(rr["Company.departments.name"], "DepartmentA1")
        assert_equal(rr["Company.departments.manager.name"], "EmployeeA1")

        rr = ResultsRow.new(@json, @view)

        assert_raise ArgumentError do
            rr["foo"]
        end

    end

    def test_to_a

        expected =  %w{DepartmentA1 EmployeeA1} << 33

        rr = ResultsRow.new(@data, @view)

        assert_equal(expected, rr.to_a)

        rr = ResultsRow.new(@json, @view)

        assert_equal(expected, rr.to_a)
    end

    def test_to_h

        rr = ResultsRow.new(@data, @view)

        expected = {
            "Company.departments.name" => "DepartmentA1",
            "Company.departments.manager.name" => "EmployeeA1",
            "Company.departments.manager.age" => 33
        }
        assert_equal(expected, rr.to_h)

        rr = ResultsRow.new(@json, @view)

        assert_equal(expected, rr.to_h)
    end


end
