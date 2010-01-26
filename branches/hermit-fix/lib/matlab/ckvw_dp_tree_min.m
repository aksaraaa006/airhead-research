
% DP merge.

function Z = ckvw_dp_tree_min(D,A,sep,dp_opt_fn)
    [m n] = size(A);
    if iscell(D)
        zD = tree_to_vector(D);
        fD = dp_opt_fn({zD},A,sep);
        Z1 = ckvw_dp_tree_min(D{1},A,sep,dp_opt_fn);
        Z2 = ckvw_dp_tree_min(D{2},A,sep,dp_opt_fn);
        ZC = [Z1 Z2];
        fC = dp_opt_fn(ZC,A,sep);
        if fC < fD
            Z = ZC;
        else
            Z = {zD};
        end
    else
        Z = {D};
    end
%
