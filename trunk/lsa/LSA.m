load termDocumentMatrix.dat;
fprintf('finished loading matrix\n');
% load the (x, y, s) data from the file into a sparse matrix
tdm = spconvert(termDocumentMatrix);

% Remove the raw data file to save space
clear termDocumentMatrix;

%terms = [ 5 10 15 20 ];
%terms = [25 50 60 70 80 90];
% terms = [100 150 200 250 300 350 400 450 500];
%terms = [ 005 010 015 020 025 050 075 085 100 125 150 175 200 ];
terms = [ 300 225 250 275 325 350 375 400 425 450 475 500 ] ;
for t = terms;
  clear U S V;
  fprintf('calculating vectors for %d dimensions\n', t);
  [U, S, V] = svds(tdm, t);
  file = sprintf('termVectors%03d.dat', t);
  save("-ascii", file, "U")
  file = sprintf('termSingularVals%03d.dat', t);
  save("-ascii", file, "S")
end
clear U S V;
