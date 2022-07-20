#ifndef __LIBLINUX_ASYNC_IO__H
#define __LIBLINUX_ASYNC_IO__H

#include <stdio.h>
#include <string.h>		/* memset() */
#include <inttypes.h>	/* uint64_t */
#include <sys/types.h>
#include <unistd.h>		/* for syscall() */
#include <sys/syscall.h>	/* for __NR_* definitions */
#include <linux/aio_abi.h>	/* for AIO types and constants */
#include <jni.h>
//#include <sys/uio.h>

#include "py_list.h"
#include "task_queue.h"

/*
 * This were good at the time of 2.6.21-rc5.mm4 ...
 */
#ifndef __NR_eventfd
#if defined(__x86_64__)
#define __NR_eventfd 284
#elif defined(__i386__)
#define __NR_eventfd 323
#else
#error Cannot detect your architecture!
#endif
#endif

#define BILLION  1000000000L

static inline u_int64_t get_tsc()
{
    u_int64_t val;
#if defined(__x86_64__) && defined(PY_RDTSC)
    u_int32_t a, d;
    __asm__ __volatile__ ("rdtsc" : "=a" (a), "=d" (d));
    val = ((u_int64_t)a) | (((u_int64_t)d) << 32);
#else
    //struct timeval now;
    //gettimeofday(&now, NULL);
	struct timespec ts;
	clock_gettime(CLOCK_REALTIME, &ts);
    val = (ts.tv_sec * BILLION) + ts.tv_nsec;
#endif
    return val;
}

enum IO_TYPE_EM {
	IO_RANDOM = 1,
	IO_SEQUENTIAL = 2,
	IO_X = 4,
};

#define MAX_RING_NAME 32  /* max ring name length */

struct user_iocb_list_t {
	struct list_head list;
    struct iocb iocb;
	long ioevent_res;  // io_get_event res
    long ioevent_res2; // io_get_event res2
    u_int64_t index;	
	u_int64_t tsc_start;
	u_int64_t tsc_end;
	u_int64_t cost_tm;
	u_int32_t io_len;  // when this event be getted, io submit count.
	u_int16_t io_type;
	u_int16_t contiguous; // io contiguous count
    JavaVM     *j_vm;  // pointer to datanode_aio_ctx_t's j_vm
    JNIEnv     *env;
    jclass     clazz;
    jmethodID  mid;
    jobject callback_obj;
};

#ifdef DEBUG_IO_TIME
struct io_offset_ns {
	u_int16_t op;
    u_int32_t length;
    u_int64_t ns;
    u_int64_t offset;
};
#define DEBUG_OFFSET_ARRAY_LENGTH  512 

typedef void (*fn_debug_offset_store)(struct io_offset_ns *, u_int16_t, struct io_offset_ns *);
typedef void (*fn_debug_offset_display)(struct io_offset_ns * u_int16_t);
typedef void (*fn_debug_offset_percent) (void *);
struct debug_st {
	struct io_offset_ns offset_array[DEBUG_OFFSET_ARRAY_LENGTH];
    fn_debug_offset_store     debug_offset_store;
	fn_debug_offset_display   debug_offset_display;
	fn_debug_offset_percent   debug_offset_percent;
};
#endif

//typedef void (*fn_filter_io) (struct datanode_aio_ctx_t *,  struct user_iocb_list_t *, u_int64_t, u_int16_t, u_int16_t *, u_int64_t *);

struct ring_io_record_st {
	u_int16_t capacity;
	u_int16_t size;
	u_int16_t pos;
	u_int16_t over_times; // count of over threshold
	u_int64_t total_cost;
	u_int32_t less;    // count that less than threshold
	u_int32_t more;    // count that more than threshold
	u_int8_t  name[MAX_RING_NAME];  // this ring's name. 
	u_int64_t *ring;
};

struct slow_disk_checker_st {
	struct ring_io_record_st ring_random;
	struct ring_io_record_st ring_write;
	struct ring_io_record_st ring_read;
	u_int64_t seq;
	u_int32_t (*slow_disk_checker)(void *, struct user_iocb_list_t *);
};

struct io_pattern_st {
	pthread_mutex_t lock;
	u_int16_t  contiguous; // 
	u_int16_t  opcode;     //0: read 1:write 
};

struct slow_disk_property_st {
	//fn_filter_io filter_io;
    void (*filter_io) (void *,  struct user_iocb_list_t *, u_int64_t, u_int16_t, u_int16_t, u_int16_t *, u_int64_t *);
};

struct datanode_aio_ctx_t {
    int32_t fd;
    int32_t event_fd;
    int32_t io_depth;
    aio_context_t ctx;
    int32_t stop_event_flag;
	int32_t finished_event_flag;
    u_int32_t submit_count;
	int32_t finished_callback_flag;
    u_int64_t event_count;
    u_int64_t write_total;
    u_int64_t read_total;
    u_int64_t total_nbytes;
    pthread_mutex_t submit_lock; //
    pthread_cond_t  submit_wait;
    pthread_mutex_t iocb_lock; 
	u_int16_t slow_disk_policy;    //policy for check slow disk
	u_int16_t is_slow_disk;
	u_int32_t slow_disk_sequen_ns; // sequential r/w overtime
	u_int32_t slow_disk_random_ns; // random r/w overtime
	float slow_disk_ignore;        // ratio r/(r+w) or w/(r+w) must over it 
	struct slow_disk_property_st  slow_disk_proerty;
	struct slow_disk_checker_st *disk_checker;
    struct user_iocb_list_t user_iocb_list;
	struct task_queue_hdr *tq_hdr;
	struct io_pattern_st  io_pattern;
    unsigned char file_name[256];
    JavaVM        *j_vm;
	jmethodID     mid_io_request;      // callback id of read/write 
	jobject       obj_slow_disk;      //
	jmethodID     mid_slow_disk;      //  callback id of slow disk check
	#ifdef DEBUG_IO_TIME
	struct debug_st *debug_st;
	#endif
};

struct task_info {
    // struct io_event *ioevent;
    //u_int16_t is_sequential;  // this io is sequential or random
    long ioevent_res;
    long ioevent_res2;
    struct user_iocb_list_t *user_iocb_list;

    // struct iocb *iocbp;
};

static inline void io_prep_pread(struct iocb *iocb, int fd, void *buf, int nr_segs,
		      int64_t offset)
{
	memset(iocb, 0, sizeof(*iocb));
	iocb->aio_fildes = fd;
	iocb->aio_lio_opcode = IOCB_CMD_PREAD;
	iocb->aio_reqprio = 0;
	iocb->aio_buf = (u_int64_t) buf;
	iocb->aio_nbytes = nr_segs;
	iocb->aio_offset = offset;
	//iocb->aio_flags = IOCB_FLAG_RESFD;
	//iocb->aio_resfd = afd;
}

static inline void io_prep_pwrite(struct iocb *iocb, int fd, void const *buf, int nr_segs,
	int64_t offset)
{
	memset(iocb, 0, sizeof(*iocb));
	iocb->aio_fildes = fd;
	iocb->aio_lio_opcode = IOCB_CMD_PWRITE;
	iocb->aio_reqprio = 0;
	iocb->aio_buf = (u_int64_t) buf;
	iocb->aio_nbytes = nr_segs;
	iocb->aio_offset = offset;
	//iocb->aio_flags = IOCB_FLAG_RESFD;
	//iocb->aio_resfd = afd;
}

static inline void io_event_setup(struct iocb *iocb, int afd)
{
       iocb->aio_flags = IOCB_FLAG_RESFD;
       iocb->aio_resfd = afd;
}

static inline int io_setup(unsigned nr, aio_context_t *ctxp)
{
	return syscall(__NR_io_setup, nr, ctxp);
}
  
static inline int io_destroy(aio_context_t ctx) 
{
	return syscall(__NR_io_destroy, ctx);
}

static inline int io_submit(aio_context_t ctx, long nr,  struct iocb **iocbpp) 
{
	return syscall(__NR_io_submit, ctx, nr, iocbpp);
}
 
static inline int io_getevents(aio_context_t ctx, long min_nr, long max_nr,
	struct io_event *events, struct timespec *timeout)
{
	return syscall(__NR_io_getevents, ctx, min_nr, max_nr, events, timeout);
}

#endif /* __LIBLINUX_ASYNC_IO__H */