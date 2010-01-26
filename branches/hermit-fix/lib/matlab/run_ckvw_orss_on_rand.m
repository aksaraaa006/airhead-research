
% Spectral clustering, using CKVW tree and ORSS seeding.
% Cheng, Kannan, Vempala, and Wang. On a Recursive Spectral
%  Algorithm for Clustering from Pairwise Similarities.
%  Loc?, Yr?.
% Ostrovsky, Rabani, Schulman, and Swamy. The Effectiveness
%  of Lloyd-type Methods for the k-Means Problem. Loc?, Yr?.

% A is matrix, rows are unitized docs, cols are terms.
% Output is col vec of tags associated with each doc.

function run_ckvw_orss_on_rand()
    run_ckvw_orss_on_id('bibs',100)
%

function run_ckvw_orss_on_id(id,k)
    for i = 0:99
        fname = sprintf('%s_%04d',id,i);
        run_ckvw_orss_on_file(fname,k);
    end
%

function run_ckvw_orss_on_file(id,k)
    OCTAVE = 1;
    % Load matrix.
    base = '/argos/shindler/';
    fin = [base 'rand_matrices/unitized_rand_20090921_' id '.csv'];
    A = load(fin);
    % Run and save cluster tags.
    front = sprintf('OUTPUT_TEMP/tag_ckor_%d_rand_20090921_',k);
    fout = [base front id '_0000.tag']
    tic
    z = cluster_spectral_ckvw_orss(A,k)';
    printf('orss %d %s %.4f\n',k,id,toc);
    if OCTAVE
        save('-ascii',fout,'z');
    else
        csvwrite(fout,z);
    end
%
