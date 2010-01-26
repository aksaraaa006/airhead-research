
% Generates a random integer in [1,n].

function Z = randrange(n,a,b)
    Z = floor(rand(a,b)*n) + 1;
%
