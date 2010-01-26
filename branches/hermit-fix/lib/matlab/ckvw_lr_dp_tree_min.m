
% l-r DP merge, for correlation.

function Z = ckvw_lr_dp_tree_min(D,A,extra,dp_opt_fn)
    if iscell(D)
        zD = tree_to_vector(D);
        fD = dp_opt_fn({zD},A,extra);
        Z1 = ckvw_lr_dp_tree_min(D{1},A,extra,dp_opt_fn);
        Z2 = ckvw_lr_dp_tree_min(D{2},A,extra,dp_opt_fn);
        ZC = [Z1 Z2];
        fC = dp_opt_fn(ZC,A,extra);
        if fC < fD
            Z = ZC;
        else
            Z = {zD};
        end
    else
        Z = {D};
    end
%
