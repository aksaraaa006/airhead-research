
% Spectral clustering.
% Cheng, Kannan, Vempala, and Wang. A Divide-and-Merge
%   Methodology for Clustering. Loc?, Yr?.

function run_ckvw_on_nsf2()
%    run_ckvw_on_id('1-3_50-50',2);
%    run_ckvw_on_id('4-5_50-50',2);
%    run_ckvw_on_id('6-7_50-50',2);
%    run_ckvw_on_id('8-9_50-50',2);
%    run_ckvw_on_id('10-13_50-50',2);
%    run_ckvw_on_id('14-15_50-50',2);
%    run_ckvw_on_id('1-3_10-90',2);
%    run_ckvw_on_id('4-5_10-90',2);
%    run_ckvw_on_id('6-7_10-90',2);
%    run_ckvw_on_id('8-9_10-90',2);
%    run_ckvw_on_id('10-13_10-90',2);
%    run_ckvw_on_id('14-15_10-90',2);
%    run_ckvw_on_id('1-3_90-10',2);
%    run_ckvw_on_id('4-5_90-10',2);
%    run_ckvw_on_id('6-7_90-10',2);
%    run_ckvw_on_id('8-9_90-10',2);
%    run_ckvw_on_id('10-13_90-10',2);
%    run_ckvw_on_id('14-15_90-10',2);
    run_ckvw_on_id('1-3-4_50-50-50',3);
    run_ckvw_on_id('5-6-7_50-50-50',3);
    run_ckvw_on_id('8-9-10_50-50-50',3);
    run_ckvw_on_id('13-14-15_50-50-50',3);
    run_ckvw_on_id('1-3-4-5_50-50-50-50',4);
    run_ckvw_on_id('6-7-8-9_50-50-50-50',4);
    run_ckvw_on_id('10-13-14-15_50-50-50-50',4);
    run_ckvw_on_id('1-3-4-5-6-7-8-9-10-13-14-15_20-20-20-20-20-20-20-20-20-20-20-20',12);
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
    fin = [base 'nsf2_matrices/unitized_nsf2_20090922_' id '.csv'];
    A = load(fin);
    %% Create clustering tree.
    tic
    T = cluster_spectral_ckvw_make_tree(A);
    printf('make_tree nsf2_20090922 %s %.4f\n',id,toc);
    % Save the k-means merge tags.
    front = sprintf('OUTPUT_TEMP/tag_ckvw_kmeans-%d_nsf2_20090922_',k);
    fout = [base front id '_0000.tag'];
    tic
    z = cluster_spectral_ckvw_merge_on_kmeans(A,T,k)';
    printf('merge_on_kmeans nsf2_20090922 %d %s %.4f\n',k,id,toc);
    if OCTAVE
        save('-ascii',fout,'z');
    else
        csvwrite(fout,z);
    end
%     % Save the correlation merge tags.
%     N = ['50'; '20'; '10'; '05'; '02'; '01'];
%     P = [0.50 0.20 0.10 0.05 0.02 0.01];
%     for i = 1:6
%         fout = [base 'OUTPUT_TEMP/tag_ckvw_corr-0p' N(i,:) '_nsf2_20090922_' id '_0000.tag'];
%         tic
%         z = cluster_spectral_ckvw_merge_on_correlation(A,T,P(i))';
%         printf('merge_on_corr nsf2_20090922 %.2f %s %.4f\n',P(i),id,toc);
%         if OCTAVE
%             save('-ascii',fout,'z');
%         else
%             csvwrite(fout,z);
%         end
%     end
%    % Save the fixed-cluster correlation merge tags.
%    N = ['02'; '01'];
%    P = [0.02 0.01];
%    for i = 1:2
%        fout = [base 'OUTPUT_TEMP/tag_ckvw_cork-0p' N(i,:) '-' sprintf('%d',k) '_nsf2_20090922_' id '_0000.tag'];
%        tic
%        z = cluster_spectral_ckvw_merge_on_correlation_fixed(A,T,P(i),k)';
%        printf('merge_on_cork nsf2_20090922 %.2f-%d %s %.4f\n',P(i),k,id,toc);
%        if OCTAVE
%            save('-ascii',fout,'z');
%        else
%            csvwrite(fout,z);
%        end
%    end
%
