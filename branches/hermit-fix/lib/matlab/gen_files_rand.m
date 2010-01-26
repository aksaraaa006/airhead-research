
% Generates a random test set as follows:
%  1. Finds a set i of k points z_i UAR on d-dim. unit
%   sphere.
%  2. Finds a set A_i of N_i points on small sphere around
%   point z_i, given by stdev. S_i.
%  3. Returns Z = set of centers, A = set of data points, q
%   the set of distances between centers.

function [Z A q] = gen_random_dataset(d,N,S)
    k = length(N);
    if length(S) ~= k
        Z = []; A = []; q = []; return
    end
    n = sum(N);
    Z = zeros(k,d);
    A = zeros(n,d);
    for i = 1:k
        Z(i,:) = randn(1,d);
        Z(i,:) = Z(i,:) ./ norm(Z(i,:));
    end
    w = 1;
    for i = 1:k
        for j = 1:N(i)
            A(w,:) = (randn(1,d).*S(i)) + Z(i,:);
            A(w,:) = A(w,:) ./ norm(A(w,:));
            w = w + 1;
        end
    end
    m = (k*(k-1)) / 2; q = zeros(m,1); w = 1;
    for i = 1:(k-1)
        for j = (i+1):k
            q(w) = norm(Z(i,:)-Z(j,:));
            w = w + 1;
        end
    end
%
