/******************************************************************************
  This file contains routines that can be bound to a Postgres backend and
  called by the backend in the process of processing queries.  The calling
  format for these routines is dictated by Postgres architecture.
******************************************************************************/

#include "postgres.h"

#include <stdlib.h>
#include <errno.h>
#include <limits.h>

#include "access/gist.h"
#include "access/skey.h"
#include "utils/builtins.h"

#include "biosegdata.h"

PG_MODULE_MAGIC;

extern int  bioseg_yyparse();
extern void bioseg_yyerror(const char *message);
extern void bioseg_scanner_init(const char *str);
extern void bioseg_scanner_finish(void);

/*
** Input/Output routines
*/
SEG   *bioseg_in(char *str);
char  *bioseg_out(SEG * seg);
SEG   *bioseg_create(int32 lower, int32 upper);
int32  bioseg_lower(SEG * seg);
int32  bioseg_upper(SEG * seg);
int32  bioseg_center(SEG * seg);

/*
** GiST support methods
*/
bool           gbioseg_consistent(GISTENTRY *entry, SEG * query, StrategyNumber strategy);
GISTENTRY     *gbioseg_compress(GISTENTRY *entry);
GISTENTRY     *gbioseg_decompress(GISTENTRY *entry);
float         *gbioseg_penalty(GISTENTRY *origentry, GISTENTRY *newentry, float *result);
GIST_SPLITVEC *gbioseg_picksplit(GistEntryVector *entryvec, GIST_SPLITVEC *v);
bool           gbioseg_leaf_consistent(SEG * key, SEG * query, StrategyNumber strategy);
bool           gbioseg_internal_consistent(SEG * key, SEG * query, StrategyNumber strategy);
SEG           *gbioseg_union(GistEntryVector *entryvec, int *sizep);
SEG           *gbioseg_binary_union(SEG * r1, SEG * r2, int *sizep);
bool          *gbioseg_same(SEG * b1, SEG * b2, bool *result);


/*
** R-tree support functions
*/
bool   bioseg_same(SEG * a, SEG * b);
bool   bioseg_contains_int(SEG * a, int *b);
bool   bioseg_contains(SEG * a, SEG * b);
bool   bioseg_contained(SEG * a, SEG * b);
bool   bioseg_overlap(SEG * a, SEG * b);
bool   bioseg_left(SEG * a, SEG * b);
bool   bioseg_over_left(SEG * a, SEG * b);
bool   bioseg_right(SEG * a, SEG * b);
bool   bioseg_over_right(SEG * a, SEG * b);
SEG   *bioseg_union(SEG * a, SEG * b);
SEG   *bioseg_inter(SEG * a, SEG * b);
void   rt_bioseg_size(SEG * a, int32 *sz);
int32  bioseg_size(SEG * a);

/*
** Various operators
*/
int32 bioseg_cmp(SEG * a, SEG * b);
bool  bioseg_lt(SEG * a, SEG * b);
bool  bioseg_le(SEG * a, SEG * b);
bool  bioseg_gt(SEG * a, SEG * b);
bool  bioseg_ge(SEG * a, SEG * b);
bool  bioseg_different(SEG * a, SEG * b);


static int get_dots(char **str);
static int get_int(char **str, int32 *result);


/*****************************************************************************
 * Input/Output functions
 *****************************************************************************/

int MAX_DIGITS = 10;

SEG *
  bioseg_in(char *str)
{
  int32 lower;
  int32 upper;

  if (!get_int(&str, &lower)) {
    return NULL;
  }

  if (str[0] == 0) {
    upper = lower;
  } else {
    if (!get_dots(&str)) {
      ereport(ERROR,
              (errcode(ERRCODE_SYNTAX_ERROR),
               errmsg("bad bioseg representation"),
               errdetail("number followed by something other than ..: %s", str)));
      return NULL;
    }

    if (!get_int(&str, &upper)) {
      ereport(ERROR,
              (errcode(ERRCODE_SYNTAX_ERROR),
               errmsg("bad bioseg representation"),
               errdetail("number\"..\" followed by something other than a number: %s", str)));
      return NULL;
    }

    if (lower > upper) {
      ereport(ERROR,
              (errcode(ERRCODE_SYNTAX_ERROR),
               errmsg("bad bioseg representation"),
               errdetail("lower limit of range greater than upper")));
      return NULL;
    }

    if (str[0] != 0) {
      ereport(ERROR,
              (errcode(ERRCODE_SYNTAX_ERROR),
               errmsg("bad bioseg representation"),
               errdetail("garbage at end of string: %s", str)));
      return NULL;
    }
  }

  {
    SEG *result = palloc(sizeof(SEG));

    result->lower = lower;
    result->upper = upper;
    return result;
  }
}

int get_int(char **strp, int32 *result) {
  char *return_pos;
  long int long_result = -1;

  if (!(*strp)[0]) {
    ereport(ERROR,
            (errcode(ERRCODE_SYNTAX_ERROR),
             errmsg("bad bioseg representation"),
             errdetail("end of string found when expecting an integer")));
    return 0;
  }

  errno = 0;

  long_result = strtol(*strp, &return_pos, 0);

  if (errno == ERANGE && (long_result == LONG_MAX || long_result == LONG_MIN)) {
    ereport(ERROR,
            (errcode(ERRCODE_SYNTAX_ERROR),
             errmsg("bad bioseg representation"),
             errdetail("integer at: %s is out of range", *strp)));
    return 0;
  }

  if (errno != 0 && long_result == 0) {
    ereport(ERROR,
            (errcode(ERRCODE_SYNTAX_ERROR),
             errmsg("bad bioseg representation"),
             errdetail("unable to read an integer: %s", strerror(errno))));
    return 0;
  }

  if (*strp == return_pos) {
    ereport(ERROR,
            (errcode(ERRCODE_SYNTAX_ERROR),
             errmsg("bad bioseg representation"),
             errdetail("no integer found at: %s", *strp)));
    return 0;
  }

  if (long_result < 1) {
    ereport(ERROR,
            (errcode(ERRCODE_SYNTAX_ERROR),
             errmsg("bad bioseg representation"),
             errdetail("integer %ld at: %s is out of range - must be >= 1", long_result, *strp)));
    return 0;
  }

  if (long_result > INT_MAX) {
    ereport(ERROR,
            (errcode(ERRCODE_SYNTAX_ERROR),
             errmsg("bad bioseg representation"),
             errdetail("integer %ld at: %s is out of range - must be <= %d", long_result, *strp, UINT_MAX)));
    return 0;
  }

  *strp = return_pos;
  *result = long_result;

  return 1;
}

int
  get_dots(char **strp)
{
  if ((*strp)[0] == '.') {
    (*strp)++;
    if ((*strp)[0] == '.') {
      (*strp)++;
      if ((*strp)[0] == '.') {
        // allow for "10...20"
        (*strp)++;
      }
      return 1;
    } else {
      return 0;
    }
  } else {
    return 0;
  }
}

char *
  bioseg_out(SEG * bioseg)
{
  char *result;
  char *p;

  if (bioseg == NULL)
    return (NULL);

  p = result = (char *) palloc(40);

  if (bioseg->lower == bioseg->upper)
    {
      /*
       * indicates that this interval was built by bioseg_in off a single point
       */
      sprintf(p, "%d", bioseg->lower);
    }
  else
    {
      sprintf(p, "%d..%d", bioseg->lower, bioseg->upper);
    }

  return (result);
}

SEG *
  bioseg_create(int32 lower, int32 upper)
{
  SEG *result = palloc(sizeof(SEG));

  result->lower = lower;
  result->upper = upper;

  return result;
}



int32
  bioseg_center(SEG * bioseg)
{
  return ((int32) bioseg->lower + (int32) bioseg->upper) / 2;
}

int32
  bioseg_lower(SEG * bioseg)
{
  return bioseg->lower;
}

int32
  bioseg_upper(SEG * bioseg)
{
  return bioseg->upper;
}


/*****************************************************************************
 * GiST functions
 *****************************************************************************/

/*
** The GiST Consistent method for biosegments
** Should return false if for all data items x below entry,
** the predicate x op query == FALSE, where op is the oper
** corresponding to strategy in the pg_amop table.
*/
bool
  gbioseg_consistent(GISTENTRY *entry,
                     SEG * query,
                     StrategyNumber strategy)
{
  /*
   * if entry is not leaf, use gbioseg_internal_consistent, else use
   * gbioseg_leaf_consistent
   */
  if (GIST_LEAF(entry))
    return (gbioseg_leaf_consistent((SEG *) DatumGetPointer(entry->key), query, strategy));
  else
    return (gbioseg_internal_consistent((SEG *) DatumGetPointer(entry->key), query, strategy));
}

/*
** The GiST Union method for biosegments
** returns the minimal bounding bioseg that encloses all the entries in entryvec
*/
SEG *
  gbioseg_union(GistEntryVector *entryvec, int *sizep)
{
  int  numranges;
  int  i;
  SEG *out = (SEG *) NULL;
  SEG *tmp;

#ifdef GIST_DEBUG
  fprintf(stderr, "union\n");
#endif

  numranges = entryvec->n;
  tmp = (SEG *) DatumGetPointer(entryvec->vector[0].key);
  *sizep = sizeof(SEG);

  for (i = 1; i < numranges; i++)
    {
      out = gbioseg_binary_union(tmp, (SEG *)
                                 DatumGetPointer(entryvec->vector[i].key),
                                 sizep);
      tmp = out;
    }

  return (out);
}

/*
** GiST Compress and Decompress methods for biosegments
** do not do anything.
*/
GISTENTRY *
  gbioseg_compress(GISTENTRY *entry)
{
  return (entry);
}

GISTENTRY *
  gbioseg_decompress(GISTENTRY *entry)
{
  return (entry);
}

/*
** The GiST Penalty method for biosegments
** As in the R-tree paper, we use change in area as our penalty metric
*/
float *
  gbioseg_penalty(GISTENTRY *origentry, GISTENTRY *newentry, float *result)
{
  SEG   *ud;
  int32 tmp1;
  int32 tmp2;

  ud = bioseg_union((SEG *) DatumGetPointer(origentry->key),
                    (SEG *) DatumGetPointer(newentry->key));
  rt_bioseg_size(ud, &tmp1);
  rt_bioseg_size((SEG *) DatumGetPointer(origentry->key), &tmp2);
  *result = tmp1 - tmp2;

#ifdef GIST_DEBUG
  fprintf(stderr, "penalty\n");
  fprintf(stderr, "\t%g\n", *result);
#endif

  return (result);
}



/*
** The GiST PickSplit method for segments
** We use Guttman's poly time split algorithm
*/
GIST_SPLITVEC *
  gbioseg_picksplit(GistEntryVector *entryvec,
                    GIST_SPLITVEC *v)
{
  OffsetNumber  i;
  OffsetNumber  j;
  SEG          *datum_alpha;
  SEG          *datum_beta;
  SEG          *datum_l;
  SEG          *datum_r;
  SEG          *union_d;
  SEG          *union_dl;
  SEG          *union_dr;
  SEG          *inter_d;
  bool          firsttime;
  int32         size_alpha;
  int32         size_beta;
  int32         size_union;
  int32         size_inter;
  int32         size_waste;
  int32         waste;
  int32         size_l;
  int32         size_r;
  int           nbytes;
  OffsetNumber  seed_1 = 1;
  OffsetNumber  seed_2 = 2;
  OffsetNumber *left;
  OffsetNumber *right;
  OffsetNumber  maxoff;

#ifdef GIST_DEBUG
  fprintf(stderr, "picksplit\n");
#endif

  maxoff = entryvec->n - 2;
  nbytes = (maxoff + 2) * sizeof(OffsetNumber);
  v->spl_left = (OffsetNumber *) palloc(nbytes);
  v->spl_right = (OffsetNumber *) palloc(nbytes);

  firsttime = true;
  waste = 0.0;

  for (i = FirstOffsetNumber; i < maxoff; i = OffsetNumberNext(i))
    {
      datum_alpha = (SEG *) DatumGetPointer(entryvec->vector[i].key);
      for (j = OffsetNumberNext(i); j <= maxoff; j = OffsetNumberNext(j))
        {
          datum_beta = (SEG *) DatumGetPointer(entryvec->vector[j].key);

          /* compute the wasted space by unioning these guys */
          /* size_waste = size_union - size_inter; */
          union_d = bioseg_union(datum_alpha, datum_beta);
          rt_bioseg_size(union_d, &size_union);
          inter_d = bioseg_inter(datum_alpha, datum_beta);
          rt_bioseg_size(inter_d, &size_inter);
          size_waste = size_union - size_inter;

          /*
           * are these a more promising split that what we've already seen?
           */
          if (size_waste > waste || firsttime)
            {
              waste = size_waste;
              seed_1 = i;
              seed_2 = j;
              firsttime = false;
            }
        }
    }

  left = v->spl_left;
  v->spl_nleft = 0;
  right = v->spl_right;
  v->spl_nright = 0;

  datum_alpha = (SEG *) DatumGetPointer(entryvec->vector[seed_1].key);
  datum_l = bioseg_union(datum_alpha, datum_alpha);
  rt_bioseg_size(datum_l, &size_l);
  datum_beta = (SEG *) DatumGetPointer(entryvec->vector[seed_2].key);
  datum_r = bioseg_union(datum_beta, datum_beta);
  rt_bioseg_size(datum_r, &size_r);

  /*
   * Now split up the regions between the two seeds.      An important property
   * of this split algorithm is that the split vector v has the indices of
   * items to be split in order in its left and right vectors.  We exploit
   * this property by doing a merge in the code that actually splits the
   * page.
   *
   * For efficiency, we also place the new index tuple in this loop. This is
   * handled at the very end, when we have placed all the existing tuples
   * and i == maxoff + 1.
   */

  maxoff = OffsetNumberNext(maxoff);
  for (i = FirstOffsetNumber; i <= maxoff; i = OffsetNumberNext(i))
    {
      /*
       * If we've already decided where to place this item, just put it on
       * the right list.      Otherwise, we need to figure out which page needs
       * the least enlargement in order to store the item.
       */

      if (i == seed_1)
        {
          *left++ = i;
          v->spl_nleft++;
          continue;
        }
      else if (i == seed_2)
        {
          *right++ = i;
          v->spl_nright++;
          continue;
        }

      /* okay, which page needs least enlargement? */
      datum_alpha = (SEG *) DatumGetPointer(entryvec->vector[i].key);
      union_dl = bioseg_union(datum_l, datum_alpha);
      union_dr = bioseg_union(datum_r, datum_alpha);
      rt_bioseg_size(union_dl, &size_alpha);
      rt_bioseg_size(union_dr, &size_beta);

      /* pick which page to add it to */
      if (size_alpha - size_l < size_beta - size_r)
        {
          datum_l = union_dl;
          size_l = size_alpha;
          *left++ = i;
          v->spl_nleft++;
        }
      else
        {
          datum_r = union_dr;
          size_r = size_alpha;
          *right++ = i;
          v->spl_nright++;
        }
    }
  *left = *right = FirstOffsetNumber; /* sentinel value, see dosplit() */

  v->spl_ldatum = PointerGetDatum(datum_l);
  v->spl_rdatum = PointerGetDatum(datum_r);

  return v;
}

/*
** Equality methods
*/
bool *
  gbioseg_same(SEG * b1, SEG * b2, bool *result)
{
  if (bioseg_same(b1, b2))
    *result = TRUE;
  else
    *result = FALSE;

#ifdef GIST_DEBUG
  fprintf(stderr, "same: %s\n", (*result ? "TRUE" : "FALSE"));
#endif

  return (result);
}

/*
** SUPPORT ROUTINES
*/
bool
  gbioseg_leaf_consistent(SEG * key,
                          SEG * query,
                          StrategyNumber strategy)
{
  bool retval;

#ifdef GIST_QUERY_DEBUG
  fprintf(stderr, "leaf_consistent, %d\n", strategy);
#endif

  switch (strategy)
    {
    case RTLeftStrategyNumber:
      retval = (bool) bioseg_left(key, query);
      break;
    case RTOverLeftStrategyNumber:
      retval = (bool) bioseg_over_left(key, query);
      break;
    case RTOverlapStrategyNumber:
      retval = (bool) bioseg_overlap(key, query);
      break;
    case RTOverRightStrategyNumber:
      retval = (bool) bioseg_over_right(key, query);
      break;
    case RTRightStrategyNumber:
      retval = (bool) bioseg_right(key, query);
      break;
    case RTSameStrategyNumber:
      retval = (bool) bioseg_same(key, query);
      break;
    case RTContainsStrategyNumber:
    case RTOldContainsStrategyNumber:
      retval = (bool) bioseg_contains(key, query);
      break;
    case RTContainedByStrategyNumber:
    case RTOldContainedByStrategyNumber:
      retval = (bool) bioseg_contained(key, query);
      break;
    default:
      retval = FALSE;
    }
  return (retval);
}

bool
  gbioseg_internal_consistent(SEG * key,
                              SEG * query,
                              StrategyNumber strategy)
{
  bool retval;

#ifdef GIST_QUERY_DEBUG
  fprintf(stderr, "internal_consistent, %d\n", strategy);
#endif

  switch (strategy)
    {
    case RTLeftStrategyNumber:
      retval = (bool) !bioseg_over_right(key, query);
      break;
    case RTOverLeftStrategyNumber:
      retval = (bool) !bioseg_right(key, query);
      break;
    case RTOverlapStrategyNumber:
      retval = (bool) bioseg_overlap(key, query);
      break;
    case RTOverRightStrategyNumber:
      retval = (bool) !bioseg_left(key, query);
      break;
    case RTRightStrategyNumber:
      retval = (bool) !bioseg_over_left(key, query);
      break;
    case RTSameStrategyNumber:
    case RTContainsStrategyNumber:
    case RTOldContainsStrategyNumber:
      retval = (bool) bioseg_contains(key, query);
      break;
    case RTContainedByStrategyNumber:
    case RTOldContainedByStrategyNumber:
      retval = (bool) bioseg_overlap(key, query);
      break;
    default:
      retval = FALSE;
    }
  return (retval);
}

SEG *
  gbioseg_binary_union(SEG * r1, SEG * r2, int *sizep)
{
  SEG *retval;

  retval = bioseg_union(r1, r2);
  *sizep = sizeof(SEG);

  return (retval);
}


bool
  bioseg_contains(SEG * a, SEG * b)
{
  return ((a->lower <= b->lower) && (a->upper >= b->upper));
}

bool
  bioseg_contained(SEG * a, SEG * b)
{
  return (bioseg_contains(b, a));
}

/*****************************************************************************
 * Operator class for R-tree indexing
 *****************************************************************************/

bool
  bioseg_same(SEG * a, SEG * b)
{
  return bioseg_cmp(a, b) == 0;
}

/*      bioseg_overlap -- does a overlap b?
 */
bool
  bioseg_overlap(SEG * a, SEG * b)
{
  return (
          ((a->upper >= b->upper) && (a->lower <= b->upper))
          ||
          ((b->upper >= a->upper) && (b->lower <= a->upper))
          );
}

/*      bioseg_overleft -- is the right edge of (a) located at or left of the right edge of (b)?
 */
bool
  bioseg_over_left(SEG * a, SEG * b)
{
  return (a->upper <= b->upper);
}

/*      bioseg_left -- is (a) entirely on the left of (b)?
 */
bool
  bioseg_left(SEG * a, SEG * b)
{
  return (a->upper < b->lower);
}

/*      bioseg_right -- is (a) entirely on the right of (b)?
 */
bool
  bioseg_right(SEG * a, SEG * b)
{
  return (a->lower > b->upper);
}

/*      bioseg_overright -- is the left edge of (a) located at or right of the left edge of (b)?
 */
bool
  bioseg_over_right(SEG * a, SEG * b)
{
  return (a->lower >= b->lower);
}


SEG *
  bioseg_union(SEG * a, SEG * b)
{
  SEG *n;

  n = (SEG *) palloc(sizeof(*n));

  /* take max of upper endpoints */
  if (a->upper > b->upper)
    {
      n->upper = a->upper;
    }
  else
    {
      n->upper = b->upper;
    }

  /* take min of lower endpoints */
  if (a->lower < b->lower)
    {
      n->lower = a->lower;
    }
  else
    {
      n->lower = b->lower;
    }

  return (n);
}


SEG *
  bioseg_inter(SEG * a, SEG * b)
{
  SEG *n;

  n = (SEG *) palloc(sizeof(*n));

  /* take min of upper endpoints */
  if (a->upper < b->upper)
    {
      n->upper = a->upper;
    }
  else
    {
      n->upper = b->upper;
    }

  /* take max of lower endpoints */
  if (a->lower > b->lower)
    {
      n->lower = a->lower;
    }
  else
    {
      n->lower = b->lower;
    }

  return (n);
}

void
  rt_bioseg_size(SEG * a, int32 *size)
{
  if (a == (SEG *) NULL || a->upper <= a->lower)
    *size = 0;
  else
    *size = (int32) (a->upper - a->lower + 1);

  return;
}

int32
  bioseg_size(SEG * a)
{
  if (a->lower < a->upper) {
    return a->upper - a->lower + 1;
  } else {
    return a->lower - a->upper + 1;
  }
}


/*****************************************************************************
 *                                 Miscellaneous operators
 *****************************************************************************/
int32
  bioseg_cmp(SEG * a, SEG * b)
{
  if (a->lower < b->lower)
    return -1;
  if (a->lower > b->lower)
    return 1;

  if (a->upper < b->upper)
    return -1;
  if (a->upper > b->upper)
    return 1;


  return 0;
}

bool
  bioseg_lt(SEG * a, SEG * b)
{
  return bioseg_cmp(a, b) < 0;
}

bool
  bioseg_le(SEG * a, SEG * b)
{
  return bioseg_cmp(a, b) <= 0;
}

bool
  bioseg_gt(SEG * a, SEG * b)
{
  return bioseg_cmp(a, b) > 0;
}

bool
  bioseg_ge(SEG * a, SEG * b)
{
  return bioseg_cmp(a, b) >= 0;
}

bool
  bioseg_different(SEG * a, SEG * b)
{
  return bioseg_cmp(a, b) != 0;
}

bool
  bioseg_contains_int(SEG * a, int *b)
{
  return ((a->lower <= *b) && (a->upper >= *b));
}
