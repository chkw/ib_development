#!/usr/local/bin/perl -w
use strict;

print "Content-type: text/html\n\n";

my %in;
if (length ($ENV{'QUERY_STRING'}) > 0){
	my $buffer = $ENV{'QUERY_STRING'};
	my @pairs = split(/&/, $buffer);
	foreach my $pair (@pairs){
		my ($name, $value) = split(/=/, $pair);
		$value =~ s/%([a-fA-F0-9][a-fA-F0-9])/pack("C", hex($1))/eg;
		$in{$name} = $value; 
	}
}

if (defined $in{name}){
	print "Hello, $in{name}.";
} else {
	print "Hello world!";
}

print "<br>";

my $now = localtime();
print "It is $now";
print "<br>";

print "Here are the parameters I saw in the query string:";
print "<br>";
foreach my $param (sort keys %in){
	print "$param	:	$in{$param}<br>";
}
