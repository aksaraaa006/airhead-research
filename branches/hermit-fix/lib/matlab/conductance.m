
% Calculates the conductance of an (S,T) split of matrix indices.

function z = conductance(A,s,t)
    m = size(A,1);
    a = c_fn(A,s,t);
    b = c_fn(A,s,1:m);
    c = c_fn(A,t,1:m);
    z = a / min(b,c);
%

function z = c_fn(A,s,t)
    z = sum(sum(A(s,:)*A(t,:)'));
%
