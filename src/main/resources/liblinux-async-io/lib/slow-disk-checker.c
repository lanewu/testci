#include <stdlib.h>
#include <stdio.h>
#include <errno.h>

#include "slow-disk-checker.h"

#define MIN_SUBMIT_EVENTS 16  /* less this digit, io_type be set IO_X. only on strategy 2 */

#ifdef DEBUG_IO_TIME
static void offset_store(struct io_offset_ns *io_offset_ns, u_int16_t index, struct io_offset_ns *src) {
    if (src == NULL || io_offset_ns == NULL)
        return;

    struct io_offset_ns *tmp = io_offset_ns + (index % DEBUG_OFFSET_ARRAY_LENGTH);
    if (tmp != NULL) {
        tmp->offset = src->offset;
        tmp->ns = src->ns;
        tmp->length = src->length;
        tmp->op = src->op;
    }
}
#endif 

static int cmpfunc (const void *a, const void *b) {
    u_int64_t x, y;
    x = *(u_int64_t *)a;
    y = *(u_int64_t *)b;

    if(x > y)
        return 1;
    else if (x < y)
       return -1;
    else 
       return 0;
}

static void offset_qsort(const char *string, u_int64_t *speciman, u_int16_t size)
{
    u_int32_t i;
    if (size <= 100) {
        return;
    }
    u_int64_t * tmp = (u_int64_t *) calloc(sizeof(u_int64_t), size);
    memcpy(tmp, speciman, size * sizeof(u_int64_t));
    u_int64_t beg_tm = get_tsc();
    qsort(tmp, size, sizeof(u_int64_t), cmpfunc);
    u_int64_t end_tm = get_tsc();

    u_int32_t index = 0;
    u_int32_t step = 0;

    step = size / 10;
    for(i = 1; i <= 10; i++) {
        index = i * size / 10 - 1;
        PY_LOG_WARNING("%s:%u(%lu)", string, index, tmp[index]);
    }

    for(i = 91; i <= 100; i++) {
        index = i * size / 100 - 1;
        PY_LOG_WARNING("%s:%u(%lu)", string, index, tmp[index]);
    }

    for(i = 991; i <= 1000; i++) {
        index = i * size / 1000 - 1;
        PY_LOG_WARNING("%s:%u(%lu)", string, index, tmp[index]);
    }
    index = size - 1;
    PY_LOG_WARNING("%s: 0(%lu) %u(%lu) qsort cost:%lu", 
                   string, tmp[0], index, tmp[index], end_tm - beg_tm);

    free(tmp);
}

#ifdef DEBUG_IO_TIME
void debug_st_init(struct datanode_aio_ctx_t *aio_ctx) 
{
    struct debug_st *debug_st = calloc(sizeof(struct debug_st), 1);
    debug_st->debug_offset_store = offset_store;
    debug_st->debug_offset_display = offset_display;
    debug_st->debug_offset_percent = offset_percent;
    aio_ctx->debug_st = debug_st;
}
#endif

#ifdef DEBUG_IO_TIME
static void offset_percent(void *dc)
{
    struct slow_disk_checker_st *disk_checker = (struct slow_disk_checker_st *)dc;
    if(disk_checker->ring_random.size == disk_checker->ring_random.capacity) {
        offset_qsort("offset random", disk_checker->ring_random.ring, disk_checker->ring_random.size);
    }

    if(disk_checker->ring_read.size == disk_checker->ring_read.capacity) {
        offset_qsort("offset read", disk_checker->ring_read.ring, disk_checker->ring_read.size);
    }

    if(disk_checker->ring_write.size == disk_checker->ring_write.capacity) {
        offset_qsort("offset write", disk_checker->ring_write.ring, disk_checker->ring_write.size);
    }
}
#endif

#ifdef DEBUG_IO_TIME
static void offset_display(struct io_offset_ns *io_offset_ns)
{
    u_int16_t index = 0;
    u_int16_t wrapper = 32;

    for(index = 0; index < DEBUG_OFFSET_ARRAY_LENGTH - wrapper; index += wrapper) {
       char msg[2048];
       char sig_msg[256];
       bzero(msg, sizeof(msg));
       int i =0;
       for(i = 0; i < wrapper; i++) {
           struct io_offset_ns *io = io_offset_ns + (index + i);
           bzero(sig_msg, sizeof(sig_msg));
           snprintf(sig_msg, 255, "{index:%d op:%d offset:[%lu %lu] ns:%lu} ", 
                    index + i, io->op, io->offset, io->offset + io->length, io->ns);
           int curr_len = strlen(msg);
           if(curr_len + strlen(sig_msg) >= 2048) {
               PY_LOG_WARNING("curr len:%d sig msg len:%ld", curr_len, strlen(sig_msg));
               break;
           }
           strncpy(msg + curr_len, sig_msg, strlen(sig_msg));
       }
       PY_LOG_WARNING("io offset ns:%s", msg);
    }
    bzero(io_offset_ns, sizeof(struct io_offset_ns) * DEBUG_OFFSET_ARRAY_LENGTH);
}
#endif



#ifdef DEBUG_IO_TIME
static void __store_debug_offset(struct datanode_aio_ctx_t *aio_ctx, struct user_iocb_list_t *user_iocb_list, u_int64_t index)
{
    if(aio_ctx->debug_st->debug_offset_store) {
        struct io_offset_ns io;
        io.offset = user_iocb_list->iocb.aio_offset;
        io.length = user_iocb_list->iocb.aio_nbytes;
        io.op = user_iocb_list->iocb.aio_lio_opcode;
        io.ns = user_iocb_list->cost_tm;
        aio_ctx->debug_st->debug_offset_store(aio_ctx->debug_st->offset_array, index, &io);
    }
}
#endif 

static void clean_io_ring(struct ring_io_record_st *io_ring) {
    bzero(io_ring->ring, sizeof(io_ring->capacity) * sizeof(u_int64_t));
    io_ring->less = 0;
    io_ring->more = 0;
    io_ring->size = 0;
    io_ring->pos = 0;
}

u_int32_t calculate_io_median_time(struct ring_io_record_st *io_ring, u_int64_t nanosec, u_int64_t threshold)
{
    u_int32_t i, ret = PY_FALSE;
    if(io_ring == NULL) {
        PY_LOG_ERROR("io_ring is null");
        return ret;
    }
    
    if(nanosec == 0)
        return PY_FALSE;

    if (io_ring->size == io_ring->capacity) {
        /* incream(remove) the oldest io */
        u_int64_t cost = io_ring->ring[io_ring->pos];
        if (cost > threshold)
            io_ring->more--;
        else
            io_ring->less--;

        io_ring->size--;
    }

    io_ring->ring[io_ring->pos] = nanosec;
    io_ring->pos = (io_ring->pos + 1) % io_ring->capacity;
    io_ring->size ++;

    if(nanosec > threshold)
        io_ring->more++;
    else
        io_ring->less++;

    if(io_ring->size < io_ring->capacity) 
        return PY_FALSE; 

    if(io_ring->less < io_ring->more) {
        io_ring->over_times ++;
        offset_qsort(io_ring->name, io_ring->ring, io_ring->size);
        clean_io_ring(io_ring); 
    } else {
        io_ring->over_times = 0;
    }

    ret = io_ring->over_times >= MAX_OVER_TIMES_SLOW_DISK ? PY_TRUE : PY_FALSE;
    return ret;
}

/* if disk is slow disk ,return 1, else return 0 */
static u_int32_t slow_disk_checker_cost(void *__aio_ctx, struct user_iocb_list_t *user_iocb_list) 
{
    struct datanode_aio_ctx_t *aio_ctx;
    struct task_info *task;
    u_int64_t avg;
    u_int64_t cost;
    u_int64_t threshold = 0;
    u_int16_t opcode; 
    struct ring_io_record_st *io_ring = NULL;

    if((__aio_ctx == NULL) || (user_iocb_list == NULL)) {
        PY_LOG_ERROR("aio_ctx or info_task is null");
        return PY_FALSE;
    }

    aio_ctx = (struct datanode_aio_ctx_t *)__aio_ctx;
    //task = (struct task_info *)__info_task;
    //struct user_iocb_list_t *user_iocb_list = (struct user_iocb_list_t *) __info_task;
       
    /* filter IO_X */
    if((user_iocb_list->io_type != IO_RANDOM) && (user_iocb_list->io_type != IO_SEQUENTIAL))
        return PY_FALSE;

    cost = user_iocb_list->cost_tm; 
    if(user_iocb_list->io_type == IO_RANDOM) {
        io_ring = &aio_ctx->disk_checker->ring_random;
        threshold = aio_ctx->slow_disk_random_ns;
    } else if(user_iocb_list->io_type == IO_SEQUENTIAL) {
        opcode = user_iocb_list->iocb.aio_lio_opcode;
        if (opcode == IOCB_CMD_PREAD) {
            io_ring = &aio_ctx->disk_checker->ring_read;
            threshold = aio_ctx->slow_disk_sequen_ns;
        } else if(opcode == IOCB_CMD_PWRITE) {
            io_ring = &aio_ctx->disk_checker->ring_write;
            threshold = aio_ctx->slow_disk_sequen_ns;
        } else {
            PY_LOG_ERROR("unknown opcde:%d", opcode);
            return PY_FALSE;
        }
    } else {
        return PY_FALSE;
    }

    if(io_ring == NULL) {
        PY_LOG_ERROR("io type:%d opcod:%d", user_iocb_list->io_type, opcode);
        return PY_FALSE;
    }

    #ifdef DEBUG_IO_TIME
    __store_debug_offset(aio_ctx, user_iocb_list, aio_ctx->disk_checker->seq);
    #endif
    aio_ctx->disk_checker->seq++;

    return calculate_io_median_time(io_ring, cost, threshold);
    
}

u_int32_t slow_disk_checker_handler(void *__aio_ctx, struct user_iocb_list_t *user_iocb_list) 
{
    struct datanode_aio_ctx_t *aio_ctx = (struct datanode_aio_ctx_t *)__aio_ctx;
    if(aio_ctx == NULL) {
        return PY_FALSE;
    }

    if(aio_ctx->slow_disk_policy == SDC_PY_AWAIT || aio_ctx->slow_disk_policy == SDC_PY_COST)  
        return slow_disk_checker_cost(__aio_ctx, user_iocb_list);
    else
        return PY_FALSE;

}

int32_t datanode_disk_checker_init(struct datanode_aio_ctx_t *aio_ctx, 
                                   u_int32_t rand_size, u_int32_t read_size, u_int32_t write_size)
{
    struct slow_disk_checker_st *disk_checker =
        (struct slow_disk_checker_st *)calloc(sizeof(struct slow_disk_checker_st), 1);
    if (disk_checker == NULL) {
        PY_LOG_ERROR("disk check init calloc error. %s", strerror(errno));
        return -1;
    }

    disk_checker->slow_disk_checker = slow_disk_checker_handler;

    disk_checker->ring_random.ring = (u_int64_t *)calloc(sizeof(u_int64_t), rand_size);
    disk_checker->ring_random.capacity = rand_size;
    strncpy(disk_checker->ring_random.name, "random ring", MAX_RING_NAME - 1);

    disk_checker->ring_read.ring = (u_int64_t *)calloc(sizeof(u_int64_t), read_size);
    disk_checker->ring_read.capacity = read_size;
    strncpy(disk_checker->ring_read.name, "read ring", MAX_RING_NAME - 1);

    disk_checker->ring_write.ring = (u_int64_t *)calloc(sizeof(u_int64_t), write_size);
    disk_checker->ring_write.capacity = write_size;
    strncpy(disk_checker->ring_write.name, "write ring", MAX_RING_NAME - 1);
    
    aio_ctx->disk_checker = disk_checker;
    return 0;
}

void datanode_disk_checker_destory(struct datanode_aio_ctx_t *aio_ctx)
{
    if(aio_ctx == NULL) {
        PY_LOG_ERROR("aio_ctx is NULL, free disk checker erro");
        return;
    }

    struct slow_disk_checker_st *disk_checker = aio_ctx->disk_checker;
    if(disk_checker == NULL) {
        PY_LOG_WARNING("storage:%s unset slow disk checker", aio_ctx->file_name);
        return;
    }

    if(disk_checker->ring_random.ring != NULL) {
        free(disk_checker->ring_random.ring);
    }

    if(disk_checker->ring_read.ring != NULL) {
        free(disk_checker->ring_read.ring);
    }

    if(disk_checker->ring_write.ring != NULL) {
        free(disk_checker->ring_write.ring);
    }

    free(disk_checker);
    disk_checker = NULL;
    aio_ctx->disk_checker = NULL;
    PY_LOG_WARNING("storage:%s disk checker be free done!", aio_ctx->file_name);
}

/* compute io await time. from io submit to 'get_event' */
void filter_io_type_await(void *__aio_ctx, 
                           struct user_iocb_list_t *user_iocb_list,
                           u_int64_t tm_event_start, u_int16_t events, u_int16_t submit_count,
                           u_int16_t *last_op, u_int64_t *last_offset)
{
    struct datanode_aio_ctx_t *aio_ctx =  (struct datanode_aio_ctx_t *)__aio_ctx;
    struct iocb *iocbp = &user_iocb_list->iocb;

    if (aio_ctx->slow_disk_policy == SDC_PY_DIS || aio_ctx->is_slow_disk == PY_TRUE)
        return;

    if (iocbp->aio_offset == *last_offset) {  // offset sequential
        user_iocb_list->io_type = IO_SEQUENTIAL;
    } else {  // offset random
        user_iocb_list->io_type = IO_RANDOM;
    }

    /* no matter tm_event_start. */
    user_iocb_list->cost_tm = (user_iocb_list->tsc_end - user_iocb_list->tsc_start);

    // update offset
    *last_offset = iocbp->aio_offset + iocbp->aio_nbytes;
    *last_op = iocbp->aio_lio_opcode;   
}

/* judege sequential or random */
void filter_io_type_cost(void *__aio_ctx, 
                           struct user_iocb_list_t *user_iocb_list,
                           u_int64_t tm_event_start, u_int16_t events, u_int16_t submit_count,
                           u_int16_t *last_op, u_int64_t *last_offset)
{
    struct datanode_aio_ctx_t *aio_ctx =  (struct datanode_aio_ctx_t *)__aio_ctx;
    struct iocb *iocbp = &user_iocb_list->iocb;

    if (aio_ctx->slow_disk_policy == SDC_PY_DIS || aio_ctx->is_slow_disk == PY_TRUE)
        return;

    user_iocb_list->io_len = submit_count;
    if (iocbp->aio_offset == *last_offset) {  // offset sequential
        if ((*last_op == iocbp->aio_lio_opcode) && (user_iocb_list->contiguous > 0)) 
            user_iocb_list->io_type = IO_SEQUENTIAL;
        else 
            user_iocb_list->io_type = IO_X;
        
    } else {  // offset random
        if (*last_op == iocbp->aio_lio_opcode) 
            user_iocb_list->io_type = IO_RANDOM;
        else 
            user_iocb_list->io_type = IO_X;
    }

    /* one case: tsc_event_start less than tsc_start. */
    u_int64_t start_tm = 0;
    if(user_iocb_list->tsc_start > tm_event_start) 
        start_tm = user_iocb_list->tsc_start;
    else 
        start_tm = tm_event_start;

    u_int64_t cost_tm = (user_iocb_list->tsc_end - start_tm) / events; 
    u_int16_t block_count = iocbp->aio_nbytes / BASE_BLOCK_SIZE;
    if(block_count > 1) 
        user_iocb_list->cost_tm = cost_tm / block_count;
    else 
        user_iocb_list->cost_tm = cost_tm;

    if(user_iocb_list->io_len < MIN_SUBMIT_EVENTS) {
        user_iocb_list->io_type = IO_X;
    }
    // update offset
    *last_offset = iocbp->aio_offset + iocbp->aio_nbytes;
    *last_op = iocbp->aio_lio_opcode;

}