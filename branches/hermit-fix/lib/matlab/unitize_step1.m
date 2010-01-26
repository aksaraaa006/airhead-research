


function unitize_step1(fin,fout)
  A = load(fin);
  for i = 1:size(A,1)
    A(i,:) = A(i,:) / norm(A(i,:));
  end
  save('-ascii',fout,'A');
%
