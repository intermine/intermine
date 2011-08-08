require 'rubygems'
require 'rubygems/specification' unless defined?(Gem::Specification)
require 'rake/testtask'
require 'rake/gempackagetask'

gem 'rdoc', '=2.1.0'
require 'rdoc/rdoc'
require 'rake/rdoctask'

gem 'darkfish-rdoc'
require 'darkfish-rdoc'

def gemspec
    @gemspec ||= begin
        Gem::Specification.load(File.expand_path('intermine-bio.gemspec'))
    end
end

task :default => :test

desc 'Start a console session'
task :console do
    system 'irb -I lib -r rubygems -r intermine/service -r intermine/bio'
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

Rake::GemPackageTask.new(gemspec) do |pkg|
    pkg.need_zip = true
    pkg.need_tar = true

end

Rake::TestTask.new do |t|
    t.libs << "test"
    t.test_files = FileList['test/unit_tests.rb']
    t.verbose = true
    t.warning = true
end

Rake::RDocTask.new do |t|
    t.title = 'Bio Extensions to the InterMine Webservice Client'
    t.rdoc_files.include 'README.rdoc'
    t.rdoc_files.include 'lib/**/*rb'
    t.main = 'README.rdoc'
    t.options += ['-SHN', '-f', 'darkfish']
end


