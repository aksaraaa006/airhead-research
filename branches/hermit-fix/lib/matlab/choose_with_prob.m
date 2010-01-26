
% Chooses from a list with some probability.
% Ostrovsky, Rabani, Schulman, and Swamy. The Effectiveness
%  of Lloyd-type Methods for the k-Means Problem. Loc?, Yr?.

% 1  choose c1, c2 w/p proportional to norm(c1-c2).^2
% 2  after i centers c1, ..., ci are chosen:
%    2-1  for each x, find 1 <= j <= i s.t. norm(x-cj) is
%          minimal
%    2-2  choose x w/p proportional to norm(x-cj_x)



% A is a matrix. First column is data to return. Second
%  column is probability.

function z = choose_with_prob(A)
    m = size(A,1);
    A(:,2) = A(:,2) / sum(A(:,2));
    t = 0; p = rand();
    for i = 1:m
        t = t + A(i,2);
        if t > p
            z = A(i,1);
            return
        end
    end
%
