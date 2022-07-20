#ifndef __SLOW_DISK_CHECKER_H__
#define __SLOW_DISK_CHECKER_H__

#include "py_base.h"
#include "liblinux-async-io.h"


#define MAX_OVER_TIMES_SLOW_DISK          3        /* over this number, we assume it's a slow disk */ 
#define SLOW_DISK_SAMPLE_IGNORE      6000     /* sample must mout than it */
#define SLOW_DISK_CHECK_INTERVAL     10000    /* slow disk check interval */


#define SLOW_DISK_CHECK_INTERVAL_AWAIT      600   /* slow disk check interval */
#define SLOW_DISK_CHECK_INTERVAL_RANDOM     1000    /* slow disk check interval */
#define SLOW_DISK_CHECK_INTERVAL_SQUEN      30000    /* slow disk check interval */
#define BASE_BLOCK_SIZE    8192                     /* 8k page siez */

enum DISK_CHECK_POLICY{
    SDC_PY_DIS = 0,       /* disable slow disk checker policy */
    SDC_PY_AWAIT = 1,     /* use await policy */
    SDC_PY_COST  = 2,     /* use io cost time policy */
};


struct slow_disk_stat_info {
    u_int16_t slow_disk_disc_count; // in countinus checking interval, slow disk be checked.
    u_int32_t io_req_total;  // interval for a checking period.
    u_int32_t over_time_random;
    u_int32_t over_time_seq_read;
    u_int32_t over_time_seq_write;
    u_int32_t io_req_random;
    u_int32_t io_req_seq_read;
    u_int32_t io_req_seq_write;
    u_int64_t io_random_total_time;
    u_int64_t io_random_average_time;
    u_int64_t io_seq_read_total_time;
    u_int64_t io_seq_read_average_time;
    u_int64_t io_seq_write_total_time;
    u_int64_t io_seq_write_average_time;
};

u_int32_t slow_disk_checker_handler(void *__aio_ctx, struct user_iocb_list_t *user_iocb_list);

int32_t datanode_disk_checker_init(struct datanode_aio_ctx_t *aio_ctx, 
                                   u_int32_t rand_size, u_int32_t read_size, u_int32_t write_size);

void datanode_disk_checker_destory(struct datanode_aio_ctx_t *aio_ctx);

void filter_io_type_await(void *__aio_ctx, 
                           struct user_iocb_list_t *user_iocb_list,
                           u_int64_t tm_event_start, u_int16_t events, u_int16_t submit_count,
                           u_int16_t *last_op, u_int64_t *last_offset);

void filter_io_type_cost(void *__aio_ctx, 
                           struct user_iocb_list_t *user_iocb_list,
                           u_int64_t tm_event_start, u_int16_t events, u_int16_t submit_count,
                           u_int16_t *last_op, u_int64_t *last_offset);
#ifdef DEBUG_IO_TIME
void debug_st_init(struct datanode_aio_ctx_t *aio_ctx) 
#endif /* DEBUG_IO_TIME */

#endif /* __SLOW_DISK_CHECKER_H__ */