
% Random clustering input.
% Input is # dims, [# per mean, stdev] per cluster.
% Output is a col vec of sample row vecs and tags (d, 1 dims).

function [Z T] = random_clustering_input(d,W)
    N = transpose(W(:,1));
    S = transpose(W(:,2));
    Z = zeros(sum(N),d);
    T = zeros(sum(N),1);
    n = size(N);
    % Q is the cluster centers.
    Q = zeros(n(2),d);
    for i = 1:n(2)
        %Q(i,:) = rand(1,d)*2 - 1;
        Q(i,:) = rand(1,d);
        Q(i,:) = Q(i,:) / norm(Q(i,:));
    end
    J = 0;
    for i = 1:n(2)
        for j = 1:N(i)
            J = J + 1;
            r = randn(1,d)*S(i) + Q(i,:);
            r = r / norm(r);
            Z(J,:) = r;
            T(J) = i;
        end
    end
%
