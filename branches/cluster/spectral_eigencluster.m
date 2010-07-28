% This is an implementation of the first version of the EigenCluster algorithm.

function assignment = spectral_eigencluster(data, k)
  % Stop partitioning the matrix if we have reached the maximal depth.
  if k == 0;
    assignment = {data};
    return;
  endif

  rows = size(data)(1);
  cols = size(data)(2);

  % Stop partitioning the matrix if we have 1 or fewer data points.
  if rows <= 1;
    assignment = {data};
    return;
  endif

  % Partition the data points in half if we only have two.
  if rows <= 2;
    assignment = {data(1,:) data(2,:)};
    return;
  endif

  printf("eigenclustering %d data points\n", rows);

  [rho, R] = compute_affinity_sums(data);
  eigen2 = compute_second_eigenvector(data, rho, R);

  printf("Sorting the eigen vector and reordering the data matrix\n");
  [sorted_eigen2, reordering] = sort(eigen2);
  re_ordered_data = spalloc(rows, cols, 0);
  for i = 1:rows;
    reordered_data(i,:) = data(reordering(i),:);
  endfor

  printf("Splitting the data matrix\n");
  cut_index = compute_spectral_cut(data, rho);

  % Short circuit at any cuts that would not partition the matrix.
  if cut_index == rows;
    assignment = { data };
    return;
  endif

  printf("Splitting at %d\n", cut_index);
  data_split_1 = reordered_data(1:cut_index,:);
  data_split_2 = reordered_data(cut_index+1:rows,:);
  %spectral_eigencluster(data_split_1, k-1)
  %spectral_eigencluster(data_split_2, k-1)
  assignment = { spectral_eigencluster(data_split_1, k-1)
                 spectral_eigencluster(data_split_2, k-1) };
endfunction

% Computes the index at which the matrix should be split.  The matrix should be
% divided such that all values up to and including the cut index are in one sub
% matrix and all other values are in the other partition. 
function cut_index = compute_spectral_cut(data, rho)
  % First compute the X and Y vectors which represent the first possible cut.
  % Also compute the same partial sums of the rho vector.
  X = data(1,:);
  rho_x = rho(1);
  Y = data(2,:);
  rho_y = rho(2);

  rows = size(data)(1);
  for row = 3:rows;
    Y += data(row,:);
    rho_y += rho(row);
  endfor

  % Compute the conductance of the first known cut.  Assume that this is the
  % smallest possible.
  mu = dot(X, Y);
  min_conductance = mu/min(rho_x, rho_y);
  min_index = 1;

  % For each possible cut, re-compute the conductance and update the X, Y, and
  % mu vectors by subtracting the row vector from Y, adding it to X, mu is
  % updated as a summation of dot products.  Also update the rho summations in
  % the same way.
  for row = 2:rows;
    rho_x += rho(row);
    rho_y -= rho(row);

    mu = mu - dot(X, data(row,:)) + (dot(Y, data(row,:)) +
                                     dot(data(row,:), data(row,:)));

    X += data(row,:);
    Y -= data(row,:);

    % Check for the new conductance value.  If it is the smallest seen so far,
    % save the conductance and the index at which it was found.
    conductance = mu/min(rho_x, rho_y);
    if conductance < min_conductance;
      min_conductance = conductance;
      min_index = row;
    endif
  endfor
  cut_index = min_index;
endfunction

% Computes the row sums of the affinity matrix.
function [rho, R] = compute_affinity_sums(data)
  printf("Computing the row sums\n");
  centroid = sum(data, 1);
  rows = size(data)(1);
  rho = zeros(rows, 1);
  for row = 1:rows;
    rho(row) += dot(centroid,data(row,:));
  endfor
  R = diag(rho.^(.5));
endfunction

% Returns a version of vector that is orthonormal with respect to other.
function orthogonal = make_orthogonal(vector, other)
  similarity = dot(other, vector);
  similarity -= other(1) * vector(1);
  similarity /= other(1);
  vector(1) = -similarity;
  magnitude = dot(vector, vector);
  orthogonal = vector./magnitude;
endfunction
  
% Computes the second largest eigen vector of the matrix (R^-1*data)(R^-1*data)'
% by using the power method.
function second_eigenvector = compute_second_eigenvector(data, rho, R)
  % Precompute the number of iterations to use.
  num_iterations = ceil(log(size(data)(1)));
  printf("Computing the second eigen vector with %d iterations.\n",
         num_iterations);

  % Precompute a matrix and vector that will be re-used several times.
  R_inv_data = R^-1 * data;
  base_vector = rho'*R^-1;

  % Initialize the eigen vector to be a random vector.
  vector = rand(size(data)(1), 1);
  for i = 1:num_iterations;
    % Orthonormalize the vector wrt rho*R^-1.
    vector = make_orthogonal(vector, base_vector);
    % Do two sparse matrix multiplications, this avoids fully computing the
    % affinity matrix.
    temp_vector = (R_inv_data)' * vector;
    vector = R_inv_data * temp_vector;
  endfor
  % End by normalizing the eigen vector.
  second_eigenvector = vector./dot(vector, vector);
endfunction
