
% Returns eig in increasing order, since eig doesn't...

function [V E] = eig_sort(A)
    [V E] = eig(A);
    e = diag(E);
    o = sortrows([-abs(e) (1:size(A,1))']);
    P = pmat(o(:,2));
    V = V * P';
    E = diag(P*e);
%
