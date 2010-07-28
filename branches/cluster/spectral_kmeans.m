% Compute the Spectral clustering algorithm specified in 
% "On Spectral Clustering: Analysis and an algorithm," Andrew Y. Ng, Micheal I.
% Jordan, and Yair Weiss.
%
% Briefly, this algorithm  reduces a set of data points by a process that is
% similar to Kernel PCA, by forming the affinity matrix with an exponential
% similarity metric and then scaling this matrix.  The dimensionality of the
% data points are then represented by the top K eigenvectors of the scaled
% affinity matrix.  Finally, the reduced data points are clustered via K-Means.
% This final step is done by storing the matrix to disk and clustering with the
% S-Space K-Means implementation.

function spectral_kmeans(data, sigma, k)
  affinity_matrix = compute_affinity(data, sigma);
  laplacian = compute_laplacian(affinity_matrix);
  reduced = reduce_space(laplacian, k);
  save spectral_kmeans_reduced.mat reduced
endfunction
  
% Computes the exponential Kernel affinity of a data matrix.  
% param data: A m by n matrix, representing m data points each of with n
%             features.
% param sigma: A scaling factor that specifies how strongly the euclidean
%              distance between points affects their affinity.
% returns a m by m matrix that is similar in spirit to data * data'
function affinity = compute_affinity(data, sigma)
  printf("computing the magnitude of each data point\n");
  sigma = 2*sigma*sigma;
  rows = size(data)(1);
  cols = size(data)(2);
  % Compute the squared magnitude of each row in the matrix.
  magnitudes = zeros(rows);
  for r = 1:rows;
    for c = find(data(r,:));
      magnitudes(r) += data(r,c).^2;
    endfor
  endfor

  printf("computing the affinity matrix\n");
  % Compute the affinity matrix by computing the kernel of each data point
  % combination.  The speed of this is improved by caching the magnitudes and
  % only comparing the sparse values between each data point.  Also, let the
  % affinity between each data point and itself be 0.
  affinity = zeros(rows, rows);
  for row1 = 1:rows;
    for row2 = (row1+1):rows;
      % Get the magnitude for the second data point.  We will iterate through
      % the non zero values in the first data point and compute the distance for
      % only those values.  The value for those indices in the second data point
      % will be subtracted from it's magnitude.  The resulting magnitude will be
      % the remaining distances between the second data point and the first data
      % point, which has a value of 0 for all remaining indices.
      altered_magnitude = magnitudes(row2);
      distance = 0;
      for c = find(data(row1,:));
        altered_magnitude -= data(row2, c)^2;
        distance += data(row1, c) * data(row2, c);
      endfor
      distance += altered_magnitude;
      distance = exp(-distance/sigma);
      affinity(row1, row2) = distance;
      affinity(row2, row1) = distance;
    endfor
  endfor
endfunction

% Computes a matrix that is similar to the laplacian matrix from an affinity
% matrix.  This corresponds to step 2 of the spectral algorithm.
% param affinity: A m by m matrix representing the similarity structure.
% returns A m by m matrix that is a scaled form of the affinity matrix.
function laplacian = compute_laplacian(affinity)
  printf("computing the laplacian matrix\n");
  D = diag(sum(affinity, 2));
  rows = size(affinity)(1);
  for i = 1:rows;
    D(i, i) = D(i, i)^(-1/2);
  endfor
  laplacian = D * affinity * D;
endfunction

function reduced = reduce_space(laplacian, k)
  printf("computing the reduced matrix\n");
  rows = size(laplacian)(1);
  [eigenvectors, eigenvalues] = eigs(laplacian, k);
  sums = sumsq(eigenvectors, 2).^(1/2);
  for r = 1:rows;
    eigenvectors(r,:) = eigenvectors(r,:)/sums(r);
  endfor
  reduced = eigenvectors;
endfunction

