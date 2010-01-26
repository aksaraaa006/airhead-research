
% Spectral clustering, using paper and Michael's bisecting
%  idea. Use bisecting cut with minimum conductance.
% Cheng, Kannan, Vempala, and Wang. On a Recursive Spectral
%  Algorithm for Clustering from Pairwise Similarities.
%  Loc?, Yr?.

% A is matrix, rows are unitized docs, cols are terms.
% Output is col vec of tags associated with each doc.

function run_ckvw_bisect_on_20ng()
%    run_ckvw_bisect_on_id('1-2_50-50',2);
%    run_ckvw_bisect_on_id('2-3_50-50',2);
%    run_ckvw_bisect_on_id('8-9_50-50',2);
%    run_ckvw_bisect_on_id('10-11_50-50',2);
%    run_ckvw_bisect_on_id('1-15_50-50',2);
%    run_ckvw_bisect_on_id('18-19_50-50',2);
%    run_ckvw_bisect_on_id('1-2_10-90',2);
%    run_ckvw_bisect_on_id('2-3_10-90',2);
%    run_ckvw_bisect_on_id('8-9_10-90',2);
%    run_ckvw_bisect_on_id('10-11_10-90',2);
%    run_ckvw_bisect_on_id('1-15_10-90',2);
%    run_ckvw_bisect_on_id('18-19_10-90',2);
%    run_ckvw_bisect_on_id('1-2_90-10',2);
%    run_ckvw_bisect_on_id('2-3_90-10',2);
%    run_ckvw_bisect_on_id('8-9_90-10',2);
%    run_ckvw_bisect_on_id('10-11_90-10',2);
%    run_ckvw_bisect_on_id('1-15_90-10',2);
%    run_ckvw_bisect_on_id('18-19_90-10',2);
%    run_ckvw_bisect_on_id('3-4-6-10_50-50-50-50',4);
%    run_ckvw_bisect_on_id('2-3-4-5-6_50-50-50-50-50',5);
%    run_ckvw_bisect_on_id('2-9-10-15-18_50-50-50-50-50',5);
    run_ckvw_bisect_on_id('2-3-4-5-6_100-100-100-100-100',5);
    run_ckvw_bisect_on_id('2-9-10-15-18_100-100-100-100-100',5);
    run_ckvw_bisect_on_id('1-5-7-8-11-12-13-14-15-17_50-50-50-50-50-50-50-50-50-50',10);
%

function run_ckvw_bisect_on_id(id,k)
    for i = 0:99
        fname = sprintf('%s_%04d',id,i);
        run_ckvw_bisect_on_file(fname,k);
    end
%

function run_ckvw_bisect_on_file(id,k)
    OCTAVE = 1;
    % Load matrix.
    base = '/argos/shindler/';
    fin = [base '20ng_matrices/unitized_20ng_20090903_' id '.csv'];
    A = load(fin);
    % Run and save cluster tags.
    front = sprintf('OUTPUT_TEMP/tag_spbi_%d_20ng_20090903_',k);
    fout = [base front id '_0000.tag']
    tic
    z = cluster_spectral_ckvw_bisect(A,k)';
    printf("bisect %d %s %.4f\n",k,id,toc);
    if OCTAVE
        save('-ascii',fout,'z');
    else
        csvwrite(fout,z);
    end
%
