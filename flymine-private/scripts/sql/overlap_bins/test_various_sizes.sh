#!/bin/bash

./test_bin_size.sh 125 | tee results_125
./test_bin_size.sh 250 | tee results_250
./test_bin_size.sh 500 | tee results_500
./test_bin_size.sh 1000 | tee results_1000
./test_bin_size.sh 2000 | tee results_2000
./test_bin_size.sh 4000 | tee results_4000
./test_bin_size.sh 8000 | tee results_8000
./test_bin_size.sh 16000 | tee results_16000
