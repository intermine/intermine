require File.dirname(__FILE__) + "/test_helper.rb"
require "intermine/query"
require "intermine/model"
require "intermine/lists"
require "intermine/service"
require "test/unit"

class Service
    def fetch(x)
        return 100
    end
end

class TestQuerySugar < Test::Unit::TestCase

    def initialize(name)
        super
        file = File.new(
            File.dirname(__FILE__) + "/data/model.json", "r")
        data = file.read
        @model = InterMine::Metadata::Model.new(data)
        @service = InterMine::Service.new("foo", "bar", @model)
        @model.send(:set_service, @service) 
        @list = InterMine::Lists::List.new({"name" => "test-list"})
    end

    def test_select_statement
        q = @model.table("Employee").select("Employee.name")
        assert_kind_of(PathQuery::Query, q)
        assert_equal(q.views, ["Employee.name"])
    end

    def test_view_expansion
        q = @model.table("Employee").select("*", "department.*")
        expected = [
         "Employee.age",
         "Employee.end",
         "Employee.fullTime",
         "Employee.id",
         "Employee.name",
         "Employee.department.id",
         "Employee.department.name"]

        assert_equal(expected, q.views.map {|x| x.to_s})
    end

    def test_sugary_null_cons
        q = @model.table("Employee").
                   select("*").
                   where(:age => nil).
                   where(:name => {"=" => nil}).
                   where(:fullTime => {"!=" => nil})

        expected = "<query model='testmodel' sortOrder='Employee.age ASC' view='Employee.age Employee.end Employee.fullTime Employee.id Employee.name'>" +
                        "<constraint op='IS NULL' code='A' path='Employee.age'/>" +
                        "<constraint op='IS NULL' code='B' path='Employee.name'/>" +
                        "<constraint op='IS NOT NULL' code='C' path='Employee.fullTime'/>" + 
                "</query>"
        
        compare_xml(expected, q.to_xml)
    end

    def test_sugary_binaries
        
        q = @model.table("Employee").
                   where(:name => "foo").
                   where(:age => 10).
                   where(:end => {"<" => 200}).
                   where("department.name" => {:contains => "foo"}).
                   where(:name => {"!=" => "bar"})

        expected = "<query model='testmodel' sortOrder='Employee.age ASC' view='Employee.age Employee.end Employee.fullTime Employee.id Employee.name'>" +
                        "<constraint op='=' code='A' value='foo' path='Employee.name'/>" + 
                        "<constraint op='=' code='B' value='10' path='Employee.age'/>" + 
                        "<constraint op='&lt;' code='C' value='200' path='Employee.end'/>" +
                        "<constraint op='CONTAINS' code='D' value='foo' path='Employee.department.name'/>" +
                        "<constraint op='!=' code='E' value='bar' path='Employee.name'/>" + 
                "</query>"
        
        compare_xml(expected, q.to_xml)
    end

    def test_sugary_binary_aliases
        
        q = @model.table("Employee").
                   where(:age => {:lt => 100}).
                   where(:age => {:gt => 200}).
                   where(:age => {:le => 300}).
                   where(:age => {:ge => 400}).
                   where("department.name" => {:eq => "foo"}).
                   where(:name => {:ne => "bar"}).
                   where(:name => {:== => "zop"})

        expected = "<query model='testmodel' sortOrder='Employee.age ASC' view='Employee.age Employee.end Employee.fullTime Employee.id Employee.name'>" +
                        "<constraint op='&lt;' code='A' value='100' path='Employee.age'/>" + 
                        "<constraint op='&gt;' code='B' value='200' path='Employee.age'/>" + 
                        "<constraint op='&lt;=' code='C' value='300' path='Employee.age'/>" + 
                        "<constraint op='&gt;=' code='D' value='400' path='Employee.age'/>" + 
                        "<constraint op='=' code='E' value='foo' path='Employee.department.name'/>" +
                        "<constraint op='!=' code='F' value='bar' path='Employee.name'/>" + 
                        "<constraint op='=' code='G' value='zop' path='Employee.name'/>" + 
                "</query>"
        
        compare_xml(expected, q.to_xml)
    end


    def test_sugary_lookups

        q = @model.table("Employee").
                   where(:department => {:lookup => "foo"}).
                   where("department.company" => {:lookup => "foo", :with => "extra"})

        expected = "<query model='testmodel' sortOrder='Employee.age ASC' view='Employee.age Employee.end Employee.fullTime Employee.id Employee.name'>" +
                        "<constraint op='LOOKUP' code='A' value='foo' path='Employee.department'/>" + 
                        "<constraint extraValue='extra' op='LOOKUP' code='B' value='foo' path='Employee.department.company'/>" + 
                "</query>"
        
        compare_xml(expected, q.to_xml)
    end

    def test_sugary_lists

        q = @model.table("Employee").
                   where(:Employee => @list).
                   where("Employee.department.manager" => {"=" => @list}).
                   where("Employee.department.manager" => {"!=" => @list}).
                   where("Employee.department" => {:in => "a list"}).
                   where("Employee.department.company" => {:not_in => "a list"})

        expected = "<query model='testmodel' sortOrder='Employee.age ASC' view='Employee.age Employee.end Employee.fullTime Employee.id Employee.name'>" +
                        "<constraint op='IN' code='A' value='test-list' path='Employee'/>" +
                        "<constraint op='IN' code='B' value='test-list' path='Employee.department.manager'/>" +
                        "<constraint op='NOT IN' code='C' value='test-list' path='Employee.department.manager'/>" +
                        "<constraint op='IN' code='D' value='a list' path='Employee.department'/>" +
                        "<constraint op='NOT IN' code='E' value='a list' path='Employee.department.company'/>" +
                "</query>"
        
        compare_xml(expected, q.to_xml)
    end

    def test_sugary_multis

        q = @model.table("Employee").
                   where(:age => 30 .. 35).
                   where("department.manager.name" => %w{zip zop zap}).
                   where(:age => {'=' => 35 .. 36}).
                   where(:end => {"!=" => 37 .. 39}).
                   where(:age => {"=" => [1, 2, 3]}).
                   where(:end => {"!=" => [3, 4, 5]}).
                   where("department.manager.age" => {:none_of => 40 .. 45}).
                   where("department.manager.end" => {:one_of => 50 .. 55}).
                   where("department.manager.age" => {:none_of => [46, 47]}).
                   where("department.manager.end" => {:one_of => [56, 57]})

        expected = "<query model='testmodel' sortOrder='Employee.age ASC' view='Employee.age Employee.end Employee.fullTime Employee.id Employee.name'>" +
                       "<constraint op='ONE OF' code='A' path='Employee.age'>" + 
                            "<value>30</value><value>31</value><value>32</value><value>33</value><value>34</value><value>35</value>" +
                       "</constraint>" + 
                       "<constraint op='ONE OF' code='B' path='Employee.department.manager.name'>" + 
                            "<value>zip</value><value>zop</value><value>zap</value>" + 
                       "</constraint>" + 
                       "<constraint op='ONE OF' code='C' path='Employee.age'>" + 
                            "<value>35</value><value>36</value>" + 
                        "</constraint>" +
                        "<constraint op='NONE OF' code='D' path='Employee.end'>" + 
                            "<value>37</value><value>38</value><value>39</value>" + 
                        "</constraint>" + 
                        "<constraint op='ONE OF' code='E' path='Employee.age'>" + 
                            "<value>1</value><value>2</value><value>3</value>" + 
                        "</constraint>" + 
                        "<constraint op='NONE OF' code='F' path='Employee.end'>" + 
                            "<value>3</value><value>4</value><value>5</value>" + 
                        "</constraint>" + 
                        "<constraint op='NONE OF' code='G' path='Employee.department.manager.age'>" +
                            "<value>40</value><value>41</value><value>42</value><value>43</value><value>44</value><value>45</value>" +
                        "</constraint>" + 
                        "<constraint op='ONE OF' code='H' path='Employee.department.manager.end'>" +
                            "<value>50</value><value>51</value><value>52</value><value>53</value><value>54</value><value>55</value>" +
                        "</constraint>" +
                        "<constraint op='NONE OF' code='I' path='Employee.department.manager.age'>" + 
                            "<value>46</value><value>47</value>" + 
                        "</constraint>" + 
                        "<constraint op='ONE OF' code='J' path='Employee.department.manager.end'>" +
                            "<value>56</value><value>57</value>" + 
                        "</constraint>" + 
                "</query>"
        
        compare_xml(expected, q.to_xml)
    end

    def test_sugary_loops

        q = @model.table("Employee").
                   where("department.company.CEO" => {:is => "department.manager"}).
                   where("department.company.CEO" => {:is_not => "department.employees"})

        expected = "<query model='testmodel' sortOrder='Employee.age ASC' view='Employee.age Employee.end Employee.fullTime Employee.id Employee.name'>" +
                        "<constraint op='=' loopPath='Employee.department.manager' code='A' path='Employee.department.company.CEO'/>" + 
                        "<constraint op='!=' loopPath='Employee.department.employees' code='B' path='Employee.department.company.CEO'/>" + 
                "</query>"
        
        compare_xml(expected, q.to_xml)
    end

    def test_sugary_subclasses
        manager = @model.table("Manager")

        q = @model.table("Employee").
                   where("department.employees" => manager).
                   where("department.employees" => {:sub_class => "CEO"})

        expected = "<query model='testmodel' sortOrder='Employee.age ASC' view='Employee.age Employee.end Employee.fullTime Employee.id Employee.name'>" +
                        "<constraint type='Manager' path='Employee.department.employees'/>" +
                        "<constraint type='CEO' path='Employee.department.employees'/>" +
                "</query>"
        
        compare_xml(expected, q.to_xml)
    end
end
