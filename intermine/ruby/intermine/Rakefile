require 'rubygems'
require 'rubygems/specification' unless defined?(Gem::Specification)
require 'rubygems/package_task'
require 'rake/testtask'

#gem 'rdoc'# , '=2.1.0'
#require 'rdoc/rdoc'
#require 'rake/rdoctask'
#
#gem 'darkfish-rdoc'
#require 'darkfish-rdoc'

def gemspec
    @gemspec ||= begin
        Gem::Specification.load(File.expand_path('intermine.gemspec'))
    end
end

task :default => :test

desc 'Start a console session'
task :console do
    system 'irb -I lib -r intermine/service'
end

desc 'Displays the current version'
task :version do 
    puts "Current version: #{gemspec.version}"
end

desc 'Installs the gem locally'
task :install => :package do
    sh "gem install pkg/#{gemspec.name}-#{gemspec.version}"
end

desc 'Release the gem'
task :release => :package do
      sh "gem push pkg/#{gemspec.name}-#{gemspec.version}.gem"
end

Gem::PackageTask.new(gemspec) do |pkg|
    pkg.need_zip = true
    pkg.need_tar = true
end



namespace :test do

    desc "Run the unit-tests"
    Rake::TestTask.new(:unit) do |t|
        t.libs << "test"
        t.test_files = FileList['test/unit_tests.rb']
        t.verbose = true
        puts "Running UNIT TESTS"
    end

    desc "Run the live integration tests"
    Rake::TestTask.new(:live) do |t|
        t.libs << "test"
        t.test_files = FileList['test/live_test.rb'] << FileList['test/live_summary_test.rb'] << FileList['test/live_results.rb']
        t.verbose = false
        puts "Running LIVE INTEGRATION TESTS"
    end

    desc "Run all tests"
    task :all => [:unit, :live]
end

desc "Run the default test-suite"
task :test => ["test:all"]

#Rake::RDocTask.new do |t|
#    t.title = 'InterMine Webservice Client Documentation'
#    t.rdoc_files.include 'README.rdoc'
#    t.rdoc_files.include 'lib/**/*rb'
#    t.main = 'README.rdoc'
#    t.options += ['-SHN', '-f', 'darkfish']
#end


