
% Generates a d-dim. vector of random integers summing to
%  approximately n.

function z = random_int_vector(d,n)
    z = randn(d,1) .^ 2;
    z = z ./ sum(z) * n;
    z = round(z);
%
