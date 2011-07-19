$LOAD_PATH << File.expand_path( File.dirname(__FILE__) + '/../lib' )
require "intermine/service"

service = Service.new("http://squirrel.flymine.org/intermine-test/service")

p service.version
p service.model.name

sum, total = 0, 0

service.model.table("Employee").
    select("name", "age").
    where(:age => {">" => 60}).
    order_by("age", "desc").each_row do |emp|
        puts emp
        sum += emp["age"]
        total += 1
end

puts "Average => #{sum/total} - #{total} employees"
puts

tok_service = Service.new("http://squirrel.flymine.org/intermine-test/service", "a1v3V1X0f3hdmaybq0l6b7Z4eVG")

q = tok_service.new_query("Employee")
q.add_views("name", "age")
q.add_constraint(:path => "Employee", :op => "IN", :value => "My-Favourite-Employees")

sum = 0
q.each_row do |emp|
    puts emp
    sum += emp["age"]
end

total = q.count
puts "Average => #{sum/total} - #{total} employees"
puts

q = tok_service.new_query("Department").add_views("name")

q.add_constraint(:path => "employees", :op => "IN", :value => "My-Favourite-Employees")

q.each_row do |emp|
    puts emp
end

sum, total,current_dep = 0, 0, nil
service.model.table("Employee").
    select(:name, :age, "department.name").
    where(:age => {"<" => 27}).
    where("department.name" => "S*").
    order_by("department.name").
    order_by(:age, :desc).
    set_logic("A or B").each_row do |emp|
    if (current_dep.nil? or current_dep != emp["department.name"])
        current_dep = emp["department.name"] 
        puts "\n#{current_dep}\n#{"-" * current_dep.length}"
    end
    puts "#{emp["name"]} (#{emp["age"]})"
    sum += emp["age"]
    total += 1
end

puts
puts "Average => #{sum/total} - #{total} employees"

service.model.table("Employee").
    select("*", "department.*").
    where(:age => {"<" => 30}).
    each_row do |emp|
        puts emp
end

Service.new("http://www.flymine.org/query/service").
    model.
    table(:Gene).
    select(:symbol, "alleles.*").
    where(:symbol => %w{zen h H bib}).
    outerjoin(:alleles).
    each_result do |gene| 
        #puts gene

        puts "#{"-" * 30}\n#{gene.symbol}: #{gene.alleles.size} Alleles"
        puts "#{gene.alleles.map {|a| a.symbol}.join(", ")}"
    end

