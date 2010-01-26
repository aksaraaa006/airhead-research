
% Changes a cell tree into a vector.

function z = tree_to_vector(T)
    z = zeros(count_leaves(T),1);
    [z j] = fill_vector(T,z,1);
%

function z = count_leaves(T)
    z = 0;
    if iscell(T)
        for i = 1:length(T)
            z = z + count_leaves(T{i});
        end
    else
        z = 1;
    end
%

function [z j] = fill_vector(T,z,j)
    if iscell(T)
        for i = 1:length(T)
            [z j] = fill_vector(T{i},z,j);
        end
    else
        z(j) = T;
        j = j + 1;
    end
%

