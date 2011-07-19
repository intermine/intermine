require 'rubygems'
require 'bundler/gem_tasks'
require 'rake/testtask'

Rake::TestTask.new do |t|
    t.libs << "test"
    t.test_files = FileList['test/unit_tests.rb']
    t.verbose = false
end
