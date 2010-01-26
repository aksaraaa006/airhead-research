
% Spectral clustering, using paper and Michael's bisecting
%  idea. Use bisecting cut with minimum conductance.
% Cheng, Kannan, Vempala, and Wang. On a Recursive Spectral
%  Algorithm for Clustering from Pairwise Similarities.
%  Loc?, Yr?.

% A is matrix, rows are unitized docs, cols are terms.
% Output is col vec of tags associated with each doc.

function run_ckvw_bisect_on_nsf2_012()
    run_ckvw_bisect_on_id('1-3_50-50',3);
    run_ckvw_bisect_on_id('4-5_50-50',3);
    run_ckvw_bisect_on_id('6-7_50-50',3);
    run_ckvw_bisect_on_id('8-9_50-50',3);
    run_ckvw_bisect_on_id('10-13_50-50',3);
    run_ckvw_bisect_on_id('14-15_50-50',3);
    run_ckvw_bisect_on_id('1-3_10-90',3);
    run_ckvw_bisect_on_id('4-5_10-90',3);
    run_ckvw_bisect_on_id('6-7_10-90',3);
    run_ckvw_bisect_on_id('8-9_10-90',3);
    run_ckvw_bisect_on_id('10-13_10-90',3);
    run_ckvw_bisect_on_id('14-15_10-90',3);
    run_ckvw_bisect_on_id('1-3_90-10',3);
    run_ckvw_bisect_on_id('4-5_90-10',3);
    run_ckvw_bisect_on_id('6-7_90-10',3);
    run_ckvw_bisect_on_id('8-9_90-10',3);
    run_ckvw_bisect_on_id('10-13_90-10',3);
    run_ckvw_bisect_on_id('14-15_90-10',3);
    run_ckvw_bisect_on_id('1-3_50-50',4);
    run_ckvw_bisect_on_id('4-5_50-50',4);
    run_ckvw_bisect_on_id('6-7_50-50',4);
    run_ckvw_bisect_on_id('8-9_50-50',4);
    run_ckvw_bisect_on_id('10-13_50-50',4);
    run_ckvw_bisect_on_id('14-15_50-50',4);
    run_ckvw_bisect_on_id('1-3_10-90',4);
    run_ckvw_bisect_on_id('4-5_10-90',4);
    run_ckvw_bisect_on_id('6-7_10-90',4);
    run_ckvw_bisect_on_id('8-9_10-90',4);
    run_ckvw_bisect_on_id('10-13_10-90',4);
    run_ckvw_bisect_on_id('14-15_10-90',4);
    run_ckvw_bisect_on_id('1-3_90-10',4);
    run_ckvw_bisect_on_id('4-5_90-10',4);
    run_ckvw_bisect_on_id('6-7_90-10',4);
    run_ckvw_bisect_on_id('8-9_90-10',4);
    run_ckvw_bisect_on_id('10-13_90-10',4);
    run_ckvw_bisect_on_id('14-15_90-10',4);
    run_ckvw_bisect_on_id('1-3_50-50',5);
    run_ckvw_bisect_on_id('4-5_50-50',5);
    run_ckvw_bisect_on_id('6-7_50-50',5);
    run_ckvw_bisect_on_id('8-9_50-50',5);
    run_ckvw_bisect_on_id('10-13_50-50',5);
    run_ckvw_bisect_on_id('14-15_50-50',5);
    run_ckvw_bisect_on_id('1-3_10-90',5);
    run_ckvw_bisect_on_id('4-5_10-90',5);
    run_ckvw_bisect_on_id('6-7_10-90',5);
    run_ckvw_bisect_on_id('8-9_10-90',5);
    run_ckvw_bisect_on_id('10-13_10-90',5);
    run_ckvw_bisect_on_id('14-15_10-90',5);
    run_ckvw_bisect_on_id('1-3_90-10',5);
    run_ckvw_bisect_on_id('4-5_90-10',5);
    run_ckvw_bisect_on_id('6-7_90-10',5);
    run_ckvw_bisect_on_id('8-9_90-10',5);
    run_ckvw_bisect_on_id('10-13_90-10',5);
    run_ckvw_bisect_on_id('14-15_90-10',5);
%    run_ckvw_bisect_on_id('1-3-4_50-50-50',3);
%    run_ckvw_bisect_on_id('5-6-7_50-50-50',3);
%    run_ckvw_bisect_on_id('8-9-10_50-50-50',3);
%    run_ckvw_bisect_on_id('13-14-15_50-50-50',3);
%    run_ckvw_bisect_on_id('1-3-4-5_50-50-50-50',4);
%    run_ckvw_bisect_on_id('6-7-8-9_50-50-50-50',4);
%    run_ckvw_bisect_on_id('10-13-14-15_50-50-50-50',4);
%    run_ckvw_bisect_on_id('1-3-4-5-6-7-8-9-10-13-14-15_20-20-20-20-20-20-20-20-20-20-20-20',12);
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
    fin = [base 'nsf2_matrices/unitized_nsf2_20090922_' id '.csv'];
    A = load(fin);
    % Run and save cluster tags.
    front = sprintf('OUTPUT_TEMP/tag_spbi_%d_nsf2_20090922_',k);
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
