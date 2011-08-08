# -*- encoding: utf-8 -*-
$:.push File.expand_path("../lib", __FILE__)
require "intermine/bio"
require "rubygems"

Gem::Specification.new do |s|
  s.name        = InterMine::Bio::NAME
  s.version     = InterMine::Bio::VERSION
  s.authors     = InterMine::Bio::AUTHORS
  s.email       = InterMine::Bio::EMAIL
  s.homepage    = InterMine::Bio::HOMEPAGE
  s.summary     = %q{Biological Extensions for the InterMine Webservice Client Library}
  s.description = File.new('README.rdoc').read
  s.add_dependency "intermine"
  s.rubyforge_project = "intermine-bio"

  s.require_paths = ["lib"]
  s.test_file     = "test/unit_tests.rb"
  s.files         = `git ls-files -- lib/*`.split("\n") + `git ls-files -- test/*`.split("\n") + Dir['[A-Z]*'] + ["contact_header.rdoc"]
  s.rdoc_options << '--title' << 'Biological Extensions to the InterMine Webservice Client' << '--main' << 'README.rdoc' << '--line-numbers'
  s.license       = 'LGPL'
  s.has_rdoc      = true
end
