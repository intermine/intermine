package WormBase::Update::Factory;
use MooseX::AbstractFactory;
        
# optional role(s) that define what the implementations should implement

#implementation_does qw/WormBase::Update::Factory/;
implementation_class_via sub { 'WormBase::Update::Staging::' . shift };

1;
