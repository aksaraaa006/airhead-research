
% Spectral clustering.
% Cheng, Kannan, Vempala, and Wang. A Divide-and-Merge
%   Methodology for Clustering. Loc?, Yr?.

function run_ckvw_on_rand()
    run_ckvw_on_id('bibs',100);
%

function run_ckvw_on_id(id,k)
    for i = 0:99
        fname = sprintf('%s_%04d',id,i)
        run_ckvw_on_file(fname,k);
    end
%

function run_ckvw_on_file(id,k)
    OCTAVE = 1;
    base = '/argos/shindler/';
    fin = [base 'rand_matrices/unitized_rand_20090921_' id '.csv'];
    A = load(fin);
    %% Create clustering tree.
    tic
    T = cluster_spectral_ckvw_make_tree(A);
    printf("make_tree rand_20090921 %s %.4f\n",id,toc);
    % Save the k-means merge tags.
    front = sprintf('OUTPUT_TEMP/tag_ckvw_kmeans-%d_rand_20090921_',k);
    fout = [base front id '_0000.tag'];
    tic
    z = cluster_spectral_ckvw_merge_on_kmeans(A,T,k)';
    printf("merge_on_kmeans rand_20090921 %d %s %.4f\n",k,id,toc);
    if OCTAVE
        save('-ascii',fout,'z');
    else
        csvwrite(fout,z);
    end
    % Save the correlation merge tags.
    N = ['50'; '20'; '10'; '05'; '02'; '01'];
    P = [0.50 0.20 0.10 0.05 0.02 0.01];
    for i = 1:6
        fout = [base 'OUTPUT_TEMP/tag_ckvw_corr-0p' N(i,:) '_rand_20090921_' id '_0000.tag'];
        tic
        z = cluster_spectral_ckvw_merge_on_correlation(A,T,P(i))';
        printf("merge_on_corr rand_20090921 %.2f %s %.4f\n",P(i),id,toc);
        if OCTAVE
            save('-ascii',fout,'z');
        else
            csvwrite(fout,z);
        end
    end
%
