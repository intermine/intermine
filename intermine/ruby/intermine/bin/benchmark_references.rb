$LOAD_PATH << File.expand_path( File.dirname(__FILE__) + '/../lib' )

require 'benchmark'
require 'intermine/service'

include Benchmark

token = "Z1a3D3U16cicCdS0T6y4bdN1SQh"
service = Service.new("localhost/intermine-test", token )
list = service.list("My-Favourite-Employees")

def do_work(managers)
    sum = managers.reduce(0) do |m, manager| 
        m + manager.department.employees.reduce(0) do |n, emp| 
            n + emp.age
        end
    end
    avg = sum / managers.reduce(0) do |m, i| 
        m + i.department.employees.size
    end
end

Benchmark.benchmark(" "*7 + CAPTION, 7, FMTSTR, ">total:", ">avg:") do |x|
    lazy_times = x.report("lazy") do
        10.times do
            managers = list.entries
            do_work(managers)
        end
    end
    some_prefetch_times = x.report("half") do
        10.times do 
            managers = list.query.select("*", "department.*").results.entries
            do_work(managers)
        end
    end

    prefetch_times = x.report("fetched") do
        10.times do 
            managers = list.query.select("*", "department.employees.*").results.entries
            do_work(managers)
        end
    end

    [lazy_times + prefetch_times + some_prefetch_times, 
        (lazy_times + prefetch_times + some_prefetch_times)/3]
end



