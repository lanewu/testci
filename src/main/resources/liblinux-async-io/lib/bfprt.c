#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include "bfprt.h"

/* insertion sort and return index of median */
static int32_t InsertSort(u_int64_t  array[], int left, int right);

/* get the index of median foreach medians */
static int GetPivotIndex(u_int64_t  array[], int left, int right);

/* split array into two parts, less pivot and more than pivot, and return pivot index */
static int Partition(u_int64_t array[], int left, int right, int pivot_index); 

void swap(u_int64_t *a, u_int64_t *b)
{
    u_int64_t temp = *a;
    *a = *b;
    *b = temp;
}

/* insertion sort and return index of median */
int InsertSort(u_int64_t array[], int left, int right)
{
    u_int64_t temp;
    int j;
    for (int i = left + 1; i <= right; i++)
    {
        temp = array[i];
        j = i - 1;
        while (j >= left && array[j] > temp)
            array[j + 1] = array[j--];
        array[j + 1] = temp;
    }

    return ((right - left) >> 1) + left;
}

/* get the index of median foreach medians */
int GetPivotIndex(u_int64_t array[], int left, int right)
{
    if (right - left < 5)
        return InsertSort(array, left, right);

    int sub_right = left - 1;
    for (int i = left; i + 4 <= right; i += 5)
    {
        int index = InsertSort(array, i, i + 4);  //找到五个元素的中位数的下标
        swap(&array[++sub_right], &array[index]);   //依次放在左侧
    }

    return BFPRT(array, left, sub_right, ((sub_right - left + 1) >> 1) + 1);
}

int Partition(u_int64_t array[], int left, int right, int pivot_index)
{
    swap(&array[pivot_index], &array[right]);  //把基准放置于末尾

    int divide_index = left;  //跟踪划分的分界线
    for (int i = left; i < right; i++)
    {
        if (array[i] < array[right])
            swap(&array[divide_index++], &array[i]);  //比基准小的都放在左侧
    }

    swap(&array[divide_index], &array[right]);  //最后把基准换回来
    return divide_index;
}

u_int64_t BFPRT(u_int64_t array[], int left, int right, const int  k)
{
    int pivot_index = GetPivotIndex(array, left, right);
    int divide_index = Partition(array, left, right, pivot_index);
    int num = divide_index - left + 1;
    if (num == k)
        return divide_index;
    else if (num > k)
        return BFPRT(array, left, divide_index - 1, k);
    else
        return BFPRT(array, divide_index + 1, right, k - num);
}
