
% Test: show some clustering metrics.

function test_show_clustering_metrics()
    [Z T] = random_clustering_input(3,[100 0.02; 100 0.02; 100 0.02]);
    a = Z(:,1);
    b = Z(:,2);
    c = Z(:,3);
    C = clustering_similarity_matrix(Z);
    plot3(a,b,c)
    C(99:102,99:102)
%
