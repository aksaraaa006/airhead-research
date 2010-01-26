
% Normalizes the rows of a matrix A using the l_p norm.

function Z = normrows(A,p)
    [m n] = size(A);
    Z = zeros(m,n);
    for i = 1:m
        Z(i,:) = A(i,:) / (sum(A(i,:).^p)^(1/p));
    end
%
