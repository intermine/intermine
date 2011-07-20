# -*- encoding: utf-8 -*-
$:.push File.expand_path("../lib", __FILE__)
require "intermine/version"

Gem::Specification.new do |s|
  s.name        = "intermine"
  s.version     = Intermine::VERSION
  s.authors     = ["Alex Kalderimis"]
  s.email       = ["dev@intermine.org"]
  s.homepage    = "http://www.intermine.org"
  s.summary     = %q{Webservice Client Library for InterMine Data-Warehouses}
  s.description = File.new('README.rdoc').read
  s.add_dependency "json"
  s.add_development_dependency "bundler", ">= 1.0.0"
  s.rubyforge_project = "intermine"

  s.files         = `git ls-files`.split("\n")
  s.test_files    = `git ls-files -- {test,spec,features}/*`.split("\n")
  s.executables   = `git ls-files -- bin/*`.split("\n").map{ |f| File.basename(f) }
  s.require_paths = ["lib"]
  s.test_files    = ["test/unit_tests.rb"]
  s.rdoc_options << '--title' << 'Rake -- Ruby Make' << '--main' << 'README' << '--line-numbers'
  s.license       = 'LGPL'
  s.has_rdoc      = true
end
