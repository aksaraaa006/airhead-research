
% Spectral clustering.
% Cheng, Kannan, Vempala, and Wang. A Divide-and-Merge
%   Methodology for Clustering. Loc?, Yr?.

function run_ckvw_on_20ng()
%    run_ckvw_on_id('1-2_50-50',2);
%    run_ckvw_on_id('2-3_50-50',2);
%    run_ckvw_on_id('8-9_50-50',2);
%    run_ckvw_on_id('10-11_50-50',2);
%    run_ckvw_on_id('1-15_50-50',2);
%    run_ckvw_on_id('18-19_50-50',2);
%    run_ckvw_on_id('1-2_10-90',2);
%    run_ckvw_on_id('2-3_10-90',2);
%    run_ckvw_on_id('8-9_10-90',2);
%    run_ckvw_on_id('10-11_10-90',2);
%    run_ckvw_on_id('1-15_10-90',2);
%    run_ckvw_on_id('18-19_10-90',2);
%    run_ckvw_on_id('1-2_90-10',2);
%    run_ckvw_on_id('2-3_90-10',2);
%    run_ckvw_on_id('8-9_90-10',2);
%    run_ckvw_on_id('10-11_90-10',2);
%    run_ckvw_on_id('1-15_90-10',2);
%    run_ckvw_on_id('18-19_90-10',2);
    run_ckvw_on_id('3-4-6-10_50-50-50-50',4);
    run_ckvw_on_id('2-3-4-5-6_50-50-50-50-50',5);
    run_ckvw_on_id('2-9-10-15-18_50-50-50-50-50',5);
    run_ckvw_on_id('2-3-4-5-6_100-100-100-100-100',5);
    run_ckvw_on_id('2-9-10-15-18_100-100-100-100-100',5);
    run_ckvw_on_id('1-5-7-8-11-12-13-14-15-17_50-50-50-50-50-50-50-50-50-50',10);
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
    fin = [base '20ng_matrices/unitized_20ng_20090903_' id '.csv'];
    A = load(fin);
    %% Create clustering tree.
    tic
    T = cluster_spectral_ckvw_make_tree(A);
    printf('make_tree 20ng_20090903 %s %.4f\n',id,toc);
    % Save the k-means merge tags.
    front = sprintf('OUTPUT_TEMP/tag_ckvw_kmeans-%d_20ng_20090903_',k);
    fout = [base front id '_0000.tag'];
    tic
    z = cluster_spectral_ckvw_merge_on_kmeans(A,T,k)';
    printf('merge_on_kmeans 20ng_20090903 %d %s %.4f\n',k,id,toc);
    if OCTAVE
        save('-ascii',fout,'z');
    else
        csvwrite(fout,z);
    end
%     % Save the correlation merge tags.
%     N = ['50'; '20'; '10'; '05'; '02'; '01'];
%     P = [0.50 0.20 0.10 0.05 0.02 0.01];
%     for i = 1:6
%         fout = [base 'OUTPUT_TEMP/tag_ckvw_corr-0p' N(i,:) '_20ng_20090903_' id '_0000.tag'];
%         tic
%         z = cluster_spectral_ckvw_merge_on_correlation(A,T,P(i))';
%         printf('merge_on_corr 20ng_20090903 %.2f %s %.4f\n',P(i),id,toc);
%         if OCTAVE
%             save('-ascii',fout,'z');
%         else
%             csvwrite(fout,z);
%         end
%     end
%    % Save the fixed-cluster correlation merge tags.
%    N = ['50'; '20'; '10'; '05'; '02'; '01'];
%    P = [0.50 0.20 0.10 0.05 0.02 0.01];
%    for i = 1:6
%        fout = [base 'OUTPUT_TEMP/tag_ckvw_cork-0p' N(i,:) '-' sprintf('%d',k) '_20ng_20090903_' id '_0000.tag'];
%        tic
%        z = cluster_spectral_ckvw_merge_on_correlation_fixed(A,T,P(i),k)';
%        printf('merge_on_cork 20ng_20090903 %.2f-%d %s %.4f\n',P(i),k,id,toc);
%        if OCTAVE
%            save('-ascii',fout,'z');
%        else
%            csvwrite(fout,z);
%        end
%    end
%
