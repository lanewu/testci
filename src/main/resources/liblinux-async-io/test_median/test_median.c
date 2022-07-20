
#include <stdlib.h>
#include <stdio.h>
#include <assert.h>
#include <string.h>
#include <time.h>

#include "py_base.h"
#include "liblinux-async-io.h"
#include "slow-disk-checker.h"


u_int32_t calculate_io_median_time(struct ring_io_record_st *io_ring, u_int64_t nanosec, u_int64_t threshold);

void test_gent_less(struct ring_io_record_st *io_ring, int less, u_int64_t threshold)
{
    srand(time(NULL));
    for(int i = 0; i < less; i++) {
        u_int64_t nanosec = rand();
        nanosec = nanosec % threshold;
        if(nanosec == 0) {
            nanosec = 1;
        }
        calculate_io_median_time(io_ring, nanosec, threshold);
    }
}

void test_gent_more(struct ring_io_record_st *io_ring, int more, u_int64_t threshold)
{
    srand(time(NULL));
    for(int i = 0; i < more; i++) {
        u_int64_t nanosec = rand();
        nanosec = nanosec + threshold;
         calculate_io_median_time(io_ring, nanosec, threshold);
    }
   
}

void test_is_slow_disk()
{
    u_int32_t ret = 0;
    u_int64_t threshold = 50000;
    u_int16_t capacity = 30000;
    struct ring_io_record_st io_ring;
    bzero(&io_ring, sizeof(io_ring));
    io_ring.capacity = capacity;
    io_ring.ring = (u_int64_t *)calloc(capacity, sizeof(u_int64_t));
    test_gent_less(&io_ring, 14999, threshold );
    test_gent_more(&io_ring, 15001, threshold);
    PY_LOG_NOTICE("les:%d more:%d over:%d", 
                   io_ring.less, io_ring.more, io_ring.over_times);
    assert(io_ring.over_times == 1);

    test_gent_less(&io_ring, 14999, threshold );
    test_gent_more(&io_ring, 15001, threshold);
    assert(io_ring.over_times == 2);

    test_gent_less(&io_ring, 14999, threshold );
    test_gent_more(&io_ring, 15000, threshold);
    assert(io_ring.over_times == 2);
    
    ret = calculate_io_median_time(&io_ring, threshold + 1, threshold);
    assert(ret == PY_TRUE);
    assert(io_ring.over_times == 3);
    free(io_ring.ring);
}

void test_clean_over_time()
{
    u_int32_t ret = 0;
    u_int64_t threshold = 50000;
    u_int16_t capacity = 30000;
    struct ring_io_record_st io_ring;
    bzero(&io_ring, sizeof(io_ring));
    io_ring.capacity = capacity;
    io_ring.ring = (u_int64_t *)calloc(capacity, sizeof(u_int64_t));
    test_gent_less(&io_ring, 14999, threshold );
    test_gent_more(&io_ring, 15001, threshold);
    PY_LOG_NOTICE("les:%d more:%d over:%d", 
                   io_ring.less, io_ring.more, io_ring.over_times);
    assert(io_ring.over_times == 1);

    test_gent_less(&io_ring, 14999, threshold );
    test_gent_more(&io_ring, 15001, threshold);
    assert(io_ring.over_times == 2);

    test_gent_less(&io_ring, 15000, threshold );
    test_gent_more(&io_ring, 14999, threshold);
    assert(io_ring.over_times == 2);
    
    ret = calculate_io_median_time(&io_ring, threshold + 1, threshold);
    assert(ret == PY_FALSE);
    assert(io_ring.over_times == 0);

    //restart 
    test_gent_less(&io_ring, 14999, threshold );
    test_gent_more(&io_ring, 15001, threshold);
    PY_LOG_NOTICE("les:%d more:%d over:%d", 
                   io_ring.less, io_ring.more, io_ring.over_times);
    assert(io_ring.over_times == 1);

    free(io_ring.ring);

}

void test_median() 
{
    u_int32_t ret = 0;
    u_int64_t threshold = 50000;
    u_int16_t capacity = 1000;
    struct ring_io_record_st io_ring;
    bzero(&io_ring, sizeof(io_ring));
    io_ring.capacity = 1000;
    io_ring.ring = (u_int64_t *)calloc(capacity, sizeof(u_int64_t));
    int less = 501;
    int more = 499;
    test_gent_less(&io_ring, less, threshold); 

    test_gent_more(&io_ring, more, threshold); 

    assert(ret == PY_FALSE);
    assert(io_ring.less == less);
    assert(io_ring.more == more);
    PY_LOG_NOTICE("fasle less:%d more:%d", io_ring.less, io_ring.more);

    ret = calculate_io_median_time(&io_ring, threshold + 1, threshold);
    assert(ret == PY_FALSE);
    ret = calculate_io_median_time(&io_ring, threshold + 2, threshold);
    assert(ret == PY_FALSE);
    assert(io_ring.over_times == 1);

    PY_LOG_NOTICE("true less:%d more:%d over:%d", io_ring.less, io_ring.more, io_ring.over_times);
    free(io_ring.ring);
}

void test_gen_random_io(struct datanode_aio_ctx_t *aio_ctx, 
                 struct user_iocb_list_t *user_iocb_list, 
                 u_int64_t event_tm, u_int16_t events, u_int16_t submit_count, 
                 u_int16_t *last_op, u_int64_t *last_offset, struct task_info *info_task, u_int16_t cost, u_int32_t count )
{
    u_int64_t tsc_start = 1000;
    for (int i = 0; i < count; i++) {
        user_iocb_list->iocb.aio_offset = *last_offset + 1;
        user_iocb_list->tsc_start = tsc_start;
        user_iocb_list->tsc_end = tsc_start + cost;
            filter_io_type_await(aio_ctx, user_iocb_list, event_tm, events,
                                 submit_count, last_op, last_offset);
        slow_disk_checker_handler(aio_ctx, info_task);

    }
}

void test_random_await_over_time()
{
    struct datanode_aio_ctx_t aio_ctx;
    bzero(&aio_ctx, sizeof(aio_ctx));
    datanode_disk_checker_init(&aio_ctx);
    aio_ctx.slow_disk_random_ns = 10000;
    aio_ctx.slow_disk_sequen_ns = 20000;
    aio_ctx.slow_disk_policy = SDC_PY_AWAIT;
    struct user_iocb_list_t user_iocb_list;
    struct task_info info_task;
    info_task.user_iocb_list = &user_iocb_list;
    u_int64_t tsc_start = 0;
    u_int64_t last_offset = 0;
    u_int16_t last_op = 0;
    u_int64_t event_tm = 0;
    u_int16_t events = 1, submit_count = 1;
    
    test_gen_random_io(&aio_ctx, &user_iocb_list, event_tm, events, 
                       submit_count, &last_op, &last_offset, 
                       &info_task, 500, SLOW_DISK_CHECK_INTERVAL_AWAIT );
    PY_LOG_DEBUG("less:%d", aio_ctx.disk_checker->ring_random.less);
    assert(aio_ctx.disk_checker->ring_random.more == 0);
    assert(aio_ctx.disk_checker->ring_random.less == SLOW_DISK_CHECK_INTERVAL_AWAIT);
    assert(aio_ctx.is_slow_disk == PY_FALSE);

    // cost more than throughold. and less equal more
    test_gen_random_io(&aio_ctx, &user_iocb_list, event_tm, events, 
                       submit_count, &last_op, &last_offset, 
                       &info_task, 10001, 500);

    PY_LOG_DEBUG("more:%d", aio_ctx.disk_checker->ring_random.more);
    PY_LOG_DEBUG("less:%d", aio_ctx.disk_checker->ring_random.less);
    assert(aio_ctx.disk_checker->ring_random.more == 500);
    assert(aio_ctx.disk_checker->ring_random.less == 500);

    //'more counter' > 'less counter' 
    test_gen_random_io(&aio_ctx, &user_iocb_list, event_tm, events, 
                       submit_count, &last_op, &last_offset, 
                       &info_task, 10001, 1);
    
    PY_LOG_DEBUG("more:%d", aio_ctx.disk_checker->ring_random.more);
    PY_LOG_DEBUG("less:%d", aio_ctx.disk_checker->ring_random.less);
    assert(aio_ctx.disk_checker->ring_random.more == 0);
    assert(aio_ctx.disk_checker->ring_random.less == 0);
    assert(aio_ctx.disk_checker->ring_random.over_times == 1);

    /*  */
    test_gen_random_io(&aio_ctx, &user_iocb_list, event_tm, events, 
                       submit_count, &last_op, &last_offset, 
                       &info_task, 10001, 501);
    PY_LOG_DEBUG("more:%d", aio_ctx.disk_checker->ring_random.more);
    PY_LOG_DEBUG("less:%d", aio_ctx.disk_checker->ring_random.less);
    assert(aio_ctx.disk_checker->ring_random.more == 501);
    assert(aio_ctx.disk_checker->ring_random.less == 0);
    assert(aio_ctx.disk_checker->ring_random.over_times == 1);

    test_gen_random_io(&aio_ctx, &user_iocb_list, event_tm, events, 
                       submit_count, &last_op, &last_offset, 
                       &info_task, 500, 498);
    PY_LOG_DEBUG("more:%d", aio_ctx.disk_checker->ring_random.more);
    PY_LOG_DEBUG("less:%d", aio_ctx.disk_checker->ring_random.less);
    assert(aio_ctx.disk_checker->ring_random.more == 501);
    assert(aio_ctx.disk_checker->ring_random.less == 498);
    assert(aio_ctx.disk_checker->ring_random.over_times == 1);

    //'more counter' > 'less counter' 
    test_gen_random_io(&aio_ctx, &user_iocb_list, event_tm, events, 
                       submit_count, &last_op, &last_offset, 
                       &info_task, 10001, 1);
    
    PY_LOG_DEBUG("more:%d", aio_ctx.disk_checker->ring_random.more);
    PY_LOG_DEBUG("less:%d", aio_ctx.disk_checker->ring_random.less);
    assert(aio_ctx.disk_checker->ring_random.more == 0);
    assert(aio_ctx.disk_checker->ring_random.less == 0);
    assert(aio_ctx.disk_checker->ring_random.over_times == 2);

    test_gen_random_io(&aio_ctx, &user_iocb_list, event_tm, events, 
                       submit_count, &last_op, &last_offset, 
                       &info_task, 10001, 1000);
 
    assert(aio_ctx.disk_checker->ring_random.over_times == 3);

}
//int py_debug_output = 1;
int main(int argc, char *argv[])
{

    py_debug_output = 1;
    py_debug_level = LOG_DEBUG;
    PY_LOG_NOTICE("-----------------start test median----------");
    test_median();

    PY_LOG_NOTICE("-----------------start test is slow disk----------");
    test_is_slow_disk();

    PY_LOG_NOTICE("-----------------start test clean over time----------");
    test_clean_over_time();

    PY_LOG_NOTICE("-----------------start test random over time----------");
    test_random_await_over_time();
}
