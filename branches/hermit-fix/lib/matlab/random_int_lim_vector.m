
% Generates a d-dim. vector of random integers summing to
%  approximately n, where entries are in [a,b].

function z = random_int_lim_vector(d,n,a,b)
    if d * a > n || d * b < n || a > b
        z = []; return
    end
    z = ones(d,1) * a;
    while sum(z) < n
        i = randrange(d,1,1);
        if z(i) < b
            z(i) = z(i) + 1;
        end
    end
%
