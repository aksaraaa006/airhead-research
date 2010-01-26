
% ORSS k-means seeding.
% Ostrovsky, Rabani, Schulman, and Swamy. The Effectiveness
%  of Lloyd-type Methods for the k-Means Problem. Loc?, Yr?.

% A is matrix, rows are unitized docs, cols are terms.
% Output is k indices into A to use for seeds.

function z = cluster_seed_orss(A,k)
    m = size(A,1);
    if k == 1
        z = 1;
    end
    if k > m
        k = m;
    end
    z = zeros(1,k);
    % Choose c1, c2 with prob. proportional to norm(c1-c2).^2.
    I2 = zeros(m*(m-1)/2,2);
    P2 = zeros(m*(m-1)/2,2);
    ci = 1;
    for i = 1:(m-1)
        for j = (i+1):m
            I2(ci,:) = [i j];
            P2(ci,:) = [ci (norm(A(i,:)-A(j,:)).^2)];
            ci = ci + 1;
        end
    end
    ci = choose_with_prob(P2);
    z(1:2) = I2(ci,:);
    % For each x, find 1 <= j <= i s.t. norm(x-cj).^2
    %  is minimal, and choose x with prob. proportional to
    %  norm(x-cj).^2.
    for ck = 2:(k-1)
        % For each x, find 1 <= j <= i s.t. norm(x-cj).^2
        %  is minimal
        x = ones(m,2) * inf;
        for i = 1:m
            for j = 1:ck
                d = norm(A(i,:)-A(z(j),:)).^2;
                if d < x(i,2)
                    x(i,1) = i; x(i,2) = d;
                end
            end
        end
        % Choose x with prob. proportional to
        %  norm(x-cj).^2.
        z(ck+1) = choose_with_prob(x);
    end
%