
% i-j DP merge, for k-means.

function Z = ckvw_ij_dp_tree_min(D,A,k,dp_opt_fn)
    if iscell(D)
        zD = tree_to_vector(D);
        if k == 1
            Z = {zD};
        else
            Z = []; fZ = inf;
            for i = 1:(k-1)
                Z1 = ckvw_ij_dp_tree_min(D{1},A,i,dp_opt_fn);
                Z2 = ckvw_ij_dp_tree_min(D{2},A,k-i,dp_opt_fn);
                ZC = [Z1 Z2];
                fC = dp_opt_fn(ZC,A,k);
                if fC < fZ
                    Z = ZC; fZ = fC;
                end
            end
        end
    else
        Z = cell(1,k); Z{1} = D;
    end
%
