# Vector-Indexed Markings

Markings store token lists in a fixed-size vector corresponding directly to the column indices of the net's incidence matrix. This aligns marking manipulation with matrix arithmetic operations while providing O(1) place-index lookup during transition evaluation and token consumption/production.
