require 'rubygems'
require 'bundler/gem_tasks'
require 'rake/testtask'

gem 'rdoc', '=2.1.0'
require 'rdoc/rdoc'
require 'rake/rdoctask'

gem 'darkfish-rdoc'
require 'darkfish-rdoc'

Rake::TestTask.new do |t|
    t.libs << "test"
    t.test_files = FileList['test/unit_tests.rb']
    t.verbose = false
end

Rake::RDocTask.new do |t|
    t.title = 'InterMine Webservice Client Documentation'
    t.rdoc_files.include 'README.rdoc'
    t.rdoc_files.include 'lib/**/*rb'
    t.main = 'README.rdoc'
    t.options += ['-SHN', '-f', 'darkfish']
end

