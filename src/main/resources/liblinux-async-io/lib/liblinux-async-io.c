
#define _GNU_SOURCE
#define __STDC_FORMAT_MACROS

#include <errno.h>
#include <fcntl.h> /* O_RDWR */
#include <jni.h>
#include <pthread.h>
#include <stdlib.h>
#include <sys/prctl.h>
#include <syslog.h>
#include <time.h>

#include "liblinux-async-io.h"
#include "slow-disk-checker.h"

//#include "bfprt.h"
#include "py_base.h"

#define AIO_OPERATION_READ 0x01
#define AIO_OPERATION_WRITE 0x02

#define AIO_NR 1024
#define TASK_QUEUE_CAPACITY 1024
#define TASK_TAKE_ONCE_MAX 64
#define MAX_IOCB_LSIT_LENGTH (TASK_QUEUE_CAPACITY + TASK_TAKE_ONCE_MAX)

#define CPU_HZ 1000                        // 1.0 GHZ
#define TSC_MILL_SECOND (CPU_HZ * 1000L)   // millsecond
#define TSC_NS_SECOND (CPU_HZ * 1000000L)  // second
#define PROFILING_WINDOW 0x010000          // 2^16

int py_debug_output = 0; //redirect into syslog
int py_debug_level = LOG_NOTICE;
/*  */
u_int64_t cpu_hz_ns = 0;

u_int32_t thread_index = 1;

static void datanode_aio_thread(long ctx_ptr);

typedef void (*aio_callback_t)(struct datanode_aio_ctx_t *aio_ctx,
                               struct iocb *iocb, long res, long res2);

u_int64_t tsc_to_ns(u_int64_t tsc) {
#if defined(__x86_64_) && defined(PY_RDTSC)
    if (cpu_hz_ns == 0) {
        PY_LOG_ERROR("-----ERRO. can't get cpu hz-------");
        exit(-1);
    }

    return tsc / cpu_hz_ns;
#else
    return tsc;
#endif
}

static int datanode_aio_icobs_init(struct datanode_aio_ctx_t *aio_ctx) {
    int i = 0;
    int ret = 0;

    ret = pthread_mutex_init(&aio_ctx->iocb_lock, NULL);
    if (ret != PY_E_SUC) {
        PY_LOG_ERROR("pthread_mutex_init iocb lock error. %d:%s", errno,
                     strerror(errno));
        goto out;
    }

    ret = pthread_mutex_init(&aio_ctx->submit_lock, NULL);
    if (ret != PY_E_SUC) {
        PY_LOG_ERROR("pthread_mutex_init submit lock error. %d:%s", errno,
                     strerror(errno));
        goto out;
    }

    ret = pthread_cond_init(&aio_ctx->submit_wait, NULL);
    if (ret != PY_E_SUC) {
        PY_LOG_ERROR("pthread_cond_init submit wait error. %d:%s", errno,
                     strerror(errno));
        goto out;
    }

    INIT_LIST_HEAD(&aio_ctx->user_iocb_list.list);

    for (i = 0; i < aio_ctx->io_depth * 2 + MAX_IOCB_LSIT_LENGTH; i++) {
        struct user_iocb_list_t *user_iocb_list =
            calloc(1, sizeof(struct user_iocb_list_t));
        if (user_iocb_list == NULL) {
            PY_LOG_ERROR("calloc  error. %d:%s", errno, strerror(errno));
            ret = -PY_E_MEM;
            break;
        }

        list_add(&user_iocb_list->list, &aio_ctx->user_iocb_list.list);
    }

out:
    return ret;
}

static int datanode_io_pattern_init(struct datanode_aio_ctx_t *aio_ctx)
{
    int ret = 0;
    ret  = pthread_mutex_init(&aio_ctx->io_pattern.lock, NULL);
    if (ret != PY_E_SUC) {
        PY_LOG_ERROR("pthread_mutex_init io pattern lock error. %d:%s", errno,
                     strerror(errno));
        return ret;
    }

    aio_ctx->io_pattern.contiguous = 0;
    aio_ctx->io_pattern.opcode = 0;

    return ret;
}

static void datanode_io_pattern_destory(struct datanode_aio_ctx_t *aio_ctx)
{
    if(aio_ctx != NULL) {
        pthread_mutex_destroy(&aio_ctx->io_pattern.lock);
    }
    
}

static void datanode_aio_icobs_destory(struct datanode_aio_ctx_t *aio_ctx) {
    uint32_t list_count = 0;
    if (aio_ctx == NULL) {
        return;
    }

    struct user_iocb_list_t *node = NULL;
    struct list_head *pos;
    if ((pthread_mutex_lock(&aio_ctx->iocb_lock)) != 0) {
        PY_LOG_ERROR("iocb　lock error.%d:%s", errno, strerror(errno));
        return;
    }

    struct list_head *n;
    list_for_each_safe(pos, n, &aio_ctx->user_iocb_list.list) {
        node = list_entry(pos, struct user_iocb_list_t, list);
        if (node != NULL) {
            list_count++;
            list_del(&node->list);
            free(node);
        }
    }
    pthread_mutex_unlock(&aio_ctx->iocb_lock);

    pthread_mutex_destroy(&aio_ctx->submit_lock);
    pthread_mutex_destroy(&aio_ctx->iocb_lock);
    pthread_cond_destroy(&aio_ctx->submit_wait);
    PY_LOG_NOTICE("pid:%u iocbs:%u be free.", getpid(), list_count);
}

JNIEXPORT jlong JNICALL Java_py_storage_async_AsyncFileAccessor_open(
    JNIEnv *env, jclass clazz, jstring jfile_name, jint io_depth) {
    int ret = 0;
    int fd;
    int i = 0;

    struct datanode_aio_ctx_t *aio_ctx =
        calloc(1, sizeof(struct datanode_aio_ctx_t));
    if (aio_ctx == NULL) {
        PY_LOG_ERROR("calloc  error. %d:%s", errno, strerror(errno));
        return -PY_E_MEM;
    }

    PY_LOG_NOTICE("aio_ctx address:0x%p\n", aio_ctx);

    JavaVM *jvm;
    jint jret = (*env)->GetJavaVM(env, &jvm);
    if (jret != 0) {
        PY_LOG_ERROR("GetJavaVM error. ret:%d\n", jret);
        return -PY_E_SYS;
    }

    aio_ctx->mid_io_request = NULL;
    aio_ctx->mid_slow_disk = NULL;
    aio_ctx->obj_slow_disk = NULL;
    aio_ctx->j_vm = jvm;

    aio_ctx->ctx = 0;
    aio_ctx->io_depth = io_depth;
    const char *file_name = (*env)->GetStringUTFChars(env, jfile_name, 0);
    if (file_name == NULL) {
        PY_LOG_ERROR("file_name is null\n");
        return -PY_EINVAL;
    }

    strncpy(aio_ctx->file_name, file_name, sizeof(aio_ctx->file_name) - 1);

    /* release file_name buffer */
    (*env)->ReleaseStringUTFChars(env, jfile_name, file_name);

    PY_LOG_NOTICE("open: file:%s io depth:%d", aio_ctx->file_name, io_depth);
    fd = open(aio_ctx->file_name, O_RDWR | O_CREAT | O_DIRECT, 0644);
    if (fd == -1) {
        PY_LOG_ERROR("open file:%s error. %d:%s", aio_ctx->file_name, errno,
                     strerror(errno));
        return -1;
    }

    aio_ctx->fd = fd;

    aio_ctx->tq_hdr = task_queue_init(TASK_QUEUE_CAPACITY, aio_ctx->file_name);
    if (aio_ctx->tq_hdr == NULL) {
        ret = PY_E_SYS;
        PY_LOG_ERROR("task queue init error.");
        goto out;
    }

    ret = datanode_aio_icobs_init(aio_ctx);
    if (ret != PY_E_SUC) {
        PY_LOG_ERROR("iocbs init error:%d", ret);
        goto out;
    }

    // ret = datanode_disk_checker_init(aio_ctx);
    // if(ret != PY_E_SUC) {
    //     PY_LOG_ERROR("disk checker init error:%d", ret);
    //     goto out;
    // }

    ret = datanode_io_pattern_init(aio_ctx);
    if(ret != PY_E_SUC) {
        PY_LOG_ERROR("io pattert init error:%d", ret);
        goto out;
    }
    #ifdef DEBUG_IO_TIME
    debug_st_init(aio_ctx);
    #endif

    aio_context_t ctx = 0;
    ret = io_setup(AIO_NR, &ctx);
    if (ret == -1) {
        goto out;
    }
    aio_ctx->ctx = ctx;

    PY_LOG_NOTICE("fd:%d evnet_fd:%d ctx:%lu\n", fd, aio_ctx->event_fd, ctx);

out:
    if (ret != 0) {
        PY_LOG_ERROR("async open error. now free aio_ctx\n");
        /* TODO free list and file name */
        if (aio_ctx != NULL) {
            free(aio_ctx);
        }
        return ret;
    } else {
        /* start event thread for one storage(file) */
        PY_LOG_NOTICE("async open finished, start event thread.");
        datanode_aio_thread((long)aio_ctx);
        return (long)aio_ctx;
    }
}

/* Attion: not thread safe. */
JNIEXPORT void JNICALL Java_py_storage_async_AsyncFileAccessor_enableDiskCheck
  (JNIEnv *env, jclass class, jlong ctx_ptr, jint sequential, jint random, jdouble quantile, jint policy, jobject callback)
{
    struct datanode_aio_ctx_t *aio_ctx = (struct datanode_aio_ctx_t *)ctx_ptr;
    if(aio_ctx == NULL) {
        PY_LOG_ERROR("storage fd is NULL") ;
        return;
    }

    if(policy == SDC_PY_DIS) {
        aio_ctx->slow_disk_policy = SDC_PY_DIS;
        goto out;
    }

    if(quantile >= 1.0 || quantile <= 0.0) {
        PY_LOG_ERROR("storage:%s quantile:%f", aio_ctx->file_name, quantile);
        goto out;
    }

    if(sequential <= 0 || random <= 0) {
        PY_LOG_ERROR("storage:%s seqential:%d random:%d", 
                      aio_ctx->file_name, sequential, random);
        goto out;
    }

    if (policy == SDC_PY_AWAIT) {
        datanode_disk_checker_init(aio_ctx, SLOW_DISK_CHECK_INTERVAL_AWAIT,
                                   SLOW_DISK_CHECK_INTERVAL_AWAIT, SLOW_DISK_CHECK_INTERVAL_AWAIT);
    }

    if (policy == SDC_PY_COST) {
        datanode_disk_checker_init(aio_ctx, SLOW_DISK_CHECK_INTERVAL_RANDOM,
                                   SLOW_DISK_CHECK_INTERVAL_SQUEN, SLOW_DISK_CHECK_INTERVAL_SQUEN);
    }

    JavaVM *jvm = aio_ctx->j_vm;
    jobject obj_callback = (*env)->NewGlobalRef(env, callback);
    jclass callback_clazz = (*env)->GetObjectClass(env, obj_callback);
    aio_ctx->obj_slow_disk = obj_callback;
    aio_ctx->mid_slow_disk = (*env)->GetMethodID(env, callback_clazz, "done", "(I)V");

    aio_ctx->slow_disk_ignore = quantile;
    aio_ctx->slow_disk_sequen_ns = sequential;
    aio_ctx->slow_disk_random_ns = random;
    aio_ctx->slow_disk_policy = policy;
    if(policy == SDC_PY_AWAIT) 
        aio_ctx->slow_disk_proerty.filter_io = filter_io_type_await;
    else if(policy == SDC_PY_COST) 
        aio_ctx->slow_disk_proerty.filter_io = filter_io_type_cost;

out:
    PY_LOG_NOTICE("storage:%s enable disk check. sequen_ns:%d random_ns:%d, ignore:%f, policy:%d",
                   aio_ctx->file_name, aio_ctx->slow_disk_sequen_ns, 
                   aio_ctx->slow_disk_random_ns, aio_ctx->slow_disk_ignore,
                   aio_ctx->slow_disk_policy);
                
    return;
}

static void callback_to_java(JavaVM *jvm, JNIEnv *env, jmethodID mid,
                             jobject obj, int error_code) {
    
    (*env)->CallIntMethod(env, obj, mid, error_code);
    (*env)->DeleteGlobalRef(env, obj);
}

static void insert_user_iocb_list(struct datanode_aio_ctx_t *aio_ctx,
                                  struct user_iocb_list_t *user_iocb_list) {
    pthread_mutex_lock(&aio_ctx->iocb_lock);
    list_add(&user_iocb_list->list, &aio_ctx->user_iocb_list.list);
    pthread_mutex_unlock(&aio_ctx->iocb_lock);
}

// TODO start new thread handle Java callback
static void aio_done_callback(struct datanode_aio_ctx_t *aio_ctx,
                              struct user_iocb_list_t *user_iocb_list) {
    if (aio_ctx == NULL) {
        PY_LOG_ERROR("aio_ctx is null");
        return;
    }

    if (user_iocb_list == NULL) {
        PY_LOG_ERROR("user iocb list is null");
        return;
    }

    struct iocb *iocbp = &(user_iocb_list->iocb);
    long res = user_iocb_list->ioevent_res;
    long res2 = user_iocb_list->ioevent_res2;

    PY_LOG_DEBUG(
        "request_type: %s, offset: %llu, length: %lld, res: %ld, res2: %ld\n",
        (iocbp->aio_lio_opcode == IOCB_CMD_PREAD) ? "READ" : "WRITE",
        iocbp->aio_offset, iocbp->aio_nbytes, res, res2);
    aio_ctx->total_nbytes += iocbp->aio_nbytes;

    int error_code = 0;
    JNIEnv *env = user_iocb_list->env;
    jobject obj = user_iocb_list->callback_obj;

    if (res != iocbp->aio_nbytes) {
        error_code = -1;
        PY_LOG_WARNING(
            "storage:%s get error event opcode:%s res:%ld  res2:%ld offset:%llu length:%lld",
            aio_ctx->file_name, (iocbp->aio_lio_opcode == IOCB_CMD_PREAD) ? "READ" : "WRITE", 
            res, res2,iocbp->aio_offset, iocbp->aio_nbytes);
    } else { /* successfuly  */
        error_code = 0; 
    }

    callback_to_java(user_iocb_list->j_vm, user_iocb_list->env,
                     user_iocb_list->mid, user_iocb_list->callback_obj,
                     error_code);

    if (user_iocb_list != NULL) {
        PY_LOG_DEBUG("------iocp:0x%p index:%lu\n", iocbp, user_iocb_list->index);
        insert_user_iocb_list(aio_ctx, user_iocb_list);
    } else {
        PY_LOG_ERROR("-------------user_iocb_list is null\n");
    }
}

static struct user_iocb_list_t *get_free_user_iocb_list(
    struct datanode_aio_ctx_t *aio_ctx) {
    if (aio_ctx == NULL) {
        PY_LOG_ERROR("aio_ctx is null");
        return NULL;
    }

    struct user_iocb_list_t *node = NULL;
    struct user_iocb_list_t *tmp_node = NULL;
    struct list_head *pos;
    if ((pthread_mutex_lock(&aio_ctx->iocb_lock)) != 0) {
        PY_LOG_ERROR("iocb　lock error.%d:%s", errno, strerror(errno));
        return NULL;
    }

    struct list_head *n;
    list_for_each_safe(pos, n, &aio_ctx->user_iocb_list.list) {
        node = list_entry(pos, struct user_iocb_list_t, list);
        if (node != NULL) {
            list_del(&node->list);
            break;
        }
    }
    pthread_mutex_unlock(&aio_ctx->iocb_lock);
    return node;
}
static int increa_submit(struct datanode_aio_ctx_t *aio_ctx, u_int64_t offset, u_int32_t size) {
    if (aio_ctx == NULL) {
        PY_LOG_ERROR("aio_ctx was NULL.....\n");
        return PY_E_SYS;
    }

    int32_t io_depth = aio_ctx->io_depth;
    if (pthread_mutex_lock(&aio_ctx->submit_lock) != 0) {
        PY_LOG_ERROR("lock error. \n");
        return -PY_E_SYS;
    }

    struct timespec ts;
    while (aio_ctx->submit_count >= io_depth) {
#if 1
        /* wait for timeout */
        clock_gettime(CLOCK_REALTIME, &ts);
        // ts.tv_nsec = 0;
        ts.tv_sec += 1;
        int ret = pthread_cond_timedwait(&aio_ctx->submit_wait,
                                         &aio_ctx->submit_lock, &ts);
        if (ret == ETIMEDOUT) {
            PY_LOG_ERROR(
                "---NOTICE----- wait over one seconds. submit_count:%u io "
                "depth:%d storage:%s offset:%ld size:%u",
                aio_ctx->submit_count, io_depth, aio_ctx->file_name, offset, size);
        }
#endif

#if 0
        /* wait for no timeout */
        int ret = pthread_cond_wait(&aio_ctx->submit_wait, &aio_ctx->submit_lock);
        if(ret != 0) {
            PY_LOG_ERROR("-----NOTICT----- wait error:%s(%d)", strerror(errno), errno);
        }
#endif
    }

    // TODO shoud use __sync_add_and_fetch()
    aio_ctx->submit_count++;

    pthread_mutex_unlock(&aio_ctx->submit_lock);

    return 0;
}

static int decrea_submit(struct datanode_aio_ctx_t *aio_ctx, int count) {
    if (aio_ctx == NULL) {
        PY_LOG_ERROR("aio_ctx was NULL.....\n");
        return PY_E_SYS;
    }

    int32_t io_depth = aio_ctx->io_depth;
    if (pthread_mutex_lock(&aio_ctx->submit_lock) != 0) {
        PY_LOG_ERROR("lock error.\n");
        return -PY_E_SYS;
    }

    if (aio_ctx->submit_count == io_depth) {
        //pthread_cond_signal(&aio_ctx->submit_wait);
        pthread_cond_broadcast(&aio_ctx->submit_wait);
    }

    // TODO shoud use __sync_sub_and_fetch()
    aio_ctx->submit_count -= count;

    pthread_mutex_unlock(&aio_ctx->submit_lock);

    return 0;
}
static long __datanode_aio_operation(JNIEnv *env, jclass clazz,
                                     jobject callback, long ctx_ptr, void *buf,
                                     long size, long offset, int op) {
    struct datanode_aio_ctx_t *aio_ctx = (struct datanode_aio_ctx_t *)ctx_ptr;
    long ret = 0;

    PY_LOG_INFO("ctx_ptr:0x%lx ctx:0x%lx fd:%d length:%ld offset:%ld \n",
                ctx_ptr, aio_ctx->ctx, aio_ctx->fd, size, offset);
    JavaVM *jvm = aio_ctx->j_vm;
    jobject callback_obj = (*env)->NewGlobalRef(env, callback);
    if (aio_ctx->mid_io_request == NULL) {
        jclass callback_clazz = (*env)->GetObjectClass(env, callback_obj);
        aio_ctx->mid_io_request = (*env)->GetMethodID(env, callback_clazz, "done", "(I)V");
        PY_LOG_NOTICE("get mid:%p by jclass:%p jobject:%p.", aio_ctx->mid_io_request,
                      callback_clazz, callback_obj);
    }

    /* wait io depth */
    if (increa_submit(aio_ctx, offset, size) != 0) {
        PY_LOG_ERROR("lock error. \n");
        ret = -PY_E_SYS;
        goto out;
    }

    /* get a node from list to save callback method */
    struct user_iocb_list_t *user_iocb_list = get_free_user_iocb_list(aio_ctx);
    if (user_iocb_list == NULL) {
        PY_LOG_ERROR("get free user iocb error");
        ret = -PY_E_MEM;
        goto release_submit;
    }

    /* save java callback method into for event handle */
    user_iocb_list->j_vm = jvm;
    user_iocb_list->callback_obj = callback_obj;
    // user_iocb_list->clazz = callback_clazz;
    user_iocb_list->mid = aio_ctx->mid_io_request;
    struct iocb *iocbp[1];
    struct iocb *iocb = &user_iocb_list->iocb;
    iocbp[0] = iocb;

    // PY_LOG_ERROR(, "user_iocb:0x%p iocb:0x%p addrss of iocb:0x%p\n address of
    // index:0x%p",
    //            user_iocb, iocb, &iocb, &(user_iocb->index));
    if (op == AIO_OPERATION_READ) {
        user_iocb_list->index = __sync_add_and_fetch(&aio_ctx->read_total, 1);
        io_prep_pread(iocb, aio_ctx->fd, buf, size, offset);
    } else if (op == AIO_OPERATION_WRITE) {
        user_iocb_list->index = __sync_add_and_fetch(&aio_ctx->write_total, 1);
        io_prep_pwrite(iocb, aio_ctx->fd, buf, size, offset);
    } else {
        ret = -PY_EINVAL;
        goto release_list;
    }
    // io_event_setup(iocb, aio_ctx->event_fd);

    /* record io pattern. how many contiguous io requset for one opcode */
    pthread_mutex_lock(&aio_ctx->io_pattern.lock);
    if(aio_ctx->io_pattern.opcode == op)
        aio_ctx->io_pattern.contiguous ++;
    else  {
        aio_ctx->io_pattern.contiguous = 0;
        aio_ctx->io_pattern.opcode = op;
    }
    user_iocb_list->contiguous = aio_ctx->io_pattern.contiguous;     
    pthread_mutex_unlock(&aio_ctx->io_pattern.lock); 

    user_iocb_list->tsc_start = get_tsc();
    iocb->aio_data = (uint64_t)user_iocb_list;
    int rc = io_submit(aio_ctx->ctx, 1, iocbp);
    if (rc == 1) {
        ret = 0;
        goto out;
    } else {
        ret = rc;
        PY_LOG_ERROR("io submit error:%d size:%ld offset:%ld op:%d\n", rc, size,
                     offset, op);
        goto release_list;
    }

release_list:
    /* TODO delete local ref */
    insert_user_iocb_list(aio_ctx, user_iocb_list);
release_submit:
    decrea_submit(aio_ctx, 1);
out:
    if (ret != 0) {
        callback_to_java(jvm, env, aio_ctx->mid_io_request, callback_obj, -1);
        PY_LOG_ERROR(
            "io submit error(%ld). storage:%s offset:%ld length:%ld request:%d",
            ret, aio_ctx->file_name, offset, size, op);
    } else {
        PY_LOG_INFO(
            "io_submit ret:%ld op:%d address of user_iocb_list:0x%p iocb:0x%p "
            "index:%lu\n",
            ret, op, user_iocb_list, iocb, user_iocb_list->index);
    }
    return ret;
}

JNIEXPORT void JNICALL Java_py_storage_async_AsyncFileAccessor_write(
    JNIEnv *env, jclass clasz, jlong ctx_ptr, jlong buf_ptr, jlong offset,
    jint length, jobject callback) {
    PY_LOG_INFO("write length:%d offset:%ld\n", length, offset);
    struct datanode_aio_ctx_t *aio_ctx = (struct datanode_aio_ctx_t *)ctx_ptr;
    void *buf = (void *)buf_ptr;
    __datanode_aio_operation(env, clasz, callback, ctx_ptr, buf, length, offset,
                             AIO_OPERATION_WRITE);
    return;
}

JNIEXPORT void JNICALL Java_py_storage_async_AsyncFileAccessor_read(
    JNIEnv *env, jclass clazz, jlong ctx_ptr, jlong buf_ptr, jlong offset,
    jint length, jobject callback) {
    PY_LOG_INFO("read length:%d offset:%ld\n", length, offset);
    struct datanode_aio_ctx_t *aio_ctx = (struct datanode_aio_ctx_t *)ctx_ptr;
    void *buf = (void *)buf_ptr;
    __datanode_aio_operation(env, clazz, callback, ctx_ptr, buf, length, offset,
                             AIO_OPERATION_READ);
    return;
}

static void *datanode_aio_callback_handler(void *ctx_ptr) {
    struct datanode_aio_ctx_t *aio_ctx = (struct datanode_aio_ctx_t *)ctx_ptr;
    char thread_name[128];
    bzero(thread_name, sizeof(thread_name));
    snprintf(thread_name, sizeof(thread_name) - 1, "aio-callback-handler-%s-%u",
             aio_ctx->file_name, thread_index++);
    prctl(PR_SET_NAME, thread_name);
    int32_t count = 0, i;
    int max_task = 64;
    struct task_node_list *task_lists[max_task];
    JNIEnv *env;
    JavaVM *jvm = aio_ctx->j_vm;

    //u_int32_t contiguous_over_time_count = 0;  //
    u_int32_t io_task_count = 0;
    struct slow_disk_stat_info disk_info;
    bzero(&disk_info, sizeof(disk_info));

    PY_LOG_NOTICE("aio callback thread:%s start.", thread_name);
    /* in this thread, env only be getted by attacthing. */
    if ((*jvm)->AttachCurrentThread(jvm, (void **)&env, NULL) != 0) {
        PY_LOG_ERROR("Failed to attach.storage:%s\n", aio_ctx->file_name);
        return NULL;
    }

    while (1) {
        count = task_queue_take(aio_ctx->tq_hdr, task_lists, max_task, 1);
        if (count > 0) {
            for (i = 0; i < count; i++) {
                struct user_iocb_list_t *user_iocb_list = (struct user_iocb_list_t *)task_lists[i];
                //struct task_info *info_task = node->data;
                user_iocb_list->env = env;
                if (aio_ctx->slow_disk_policy != SDC_PY_DIS && aio_ctx->is_slow_disk == PY_FALSE) {
                    if (aio_ctx->disk_checker != NULL && aio_ctx->disk_checker->slow_disk_checker != NULL) {
                        if (aio_ctx->disk_checker->slow_disk_checker(aio_ctx, user_iocb_list) == PY_TRUE) {
                            /* set disk to slow disk */
                            aio_ctx->is_slow_disk = PY_TRUE;
                            callback_to_java(jvm, env, aio_ctx->mid_slow_disk, aio_ctx->obj_slow_disk, -1);
                            PY_LOG_NOTICE("storage:%s be checked in slow disk index:%lu",
                                          aio_ctx->file_name, user_iocb_list->index);
                        }
                    } else {
                        PY_LOG_ERROR("slow disk strategy:%d, but disk_checker os slow_disk_checker is null.", 
                                     aio_ctx->slow_disk_policy);
                    }

                    #ifdef DEBUG_IO_TIME
                    io_task_count++;
                    if (io_task_count >= SLOW_DISK_CHECK_INTERVAL) {
                        struct slow_disk_checker_st *disk_checker = aio_ctx->disk_checker;
                        PY_LOG_WARNING("random cost:%u:%u read cost:%u:%u write cost:%u:%u", 
                            disk_checker->ring_random.less, disk_checker->ring_random.more,
                            disk_checker->ring_read.less, disk_checker->ring_read.more,
                            disk_checker->ring_write.less, disk_checker->ring_write.more);

                        #ifdef DEBUG_IO_OFFSET
                        if(aio_ctx->debug_st->debug_offset_display) {
                            aio_ctx->debug_st->debug_offset_display(aio_ctx->debug_st->offset_array);
                        }
                        #endif
                        if(aio_ctx->debug_st->debug_offset_percent) {
                            aio_ctx->debug_st->debug_offset_percent(aio_ctx->disk_checker);
                        }
                        io_task_count = 0;
                    }
                    #endif /* DEBUG_IO_TIME */
                }

                aio_done_callback(aio_ctx, user_iocb_list);
            }
        } else if (count < 0) {
            PY_LOG_ERROR("take task error.%s", aio_ctx->tq_hdr->name);
        } else if (count == 0) {
            PY_LOG_INFO("timeout");
            /* check exit thread condition */
            if ((aio_ctx->stop_event_flag == 1) && (aio_ctx->finished_event_flag == 1) && (aio_ctx->tq_hdr->size == 0)) {
                PY_LOG_NOTICE("callback thread:%s get stop evnet flag.", thread_name);
                if (aio_ctx->submit_count == 0) {
                    /* submit io had already been completed */
                    PY_LOG_NOTICE(
                        "aio callback handle thread exit. queue size:%u",
                        aio_ctx->tq_hdr->size);
                    break;
                }
            }
        }
    }
    if(aio_ctx->mid_slow_disk != NULL && aio_ctx->is_slow_disk == PY_FALSE) {
        (*env)->DeleteGlobalRef(env, aio_ctx->obj_slow_disk);
    }
    (*jvm)->DetachCurrentThread(jvm);
    aio_ctx->finished_callback_flag = 1;
    PY_LOG_NOTICE("callback thread finised:%s", thread_name);
    /// thread finished
    return (void *)0;
}

static void *datanode_aio_event_handler(void *ctx_ptr) {
    struct datanode_aio_ctx_t *aio_ctx = (struct datanode_aio_ctx_t *)ctx_ptr;
    long io_depth = aio_ctx->io_depth;
    long max_nr_event = 8;  
    struct io_event *events = calloc(max_nr_event, sizeof(struct io_event));
    struct timespec tms;
    u_int32_t j = 0;
    u_int32_t submit_count = 0; 
    JNIEnv *env;
    JavaVM *jvm = aio_ctx->j_vm;
    char thread_name[128];
    bzero(thread_name, sizeof(thread_name));
    snprintf(thread_name, sizeof(thread_name) - 1, "aio-event-handler-%s-%d",
             aio_ctx->file_name, aio_ctx->fd);
    prctl(PR_SET_NAME, thread_name);
    int32_t poll_ret = 0;
    /* in this thread, env only be getted by attacthing. */
    if ((*jvm)->AttachCurrentThread(jvm, (void **)&env, NULL) != 0) {
        PY_LOG_ERROR("Failed to attach.storage:%s\n", aio_ctx->file_name);
        return NULL; 
    }

    PY_LOG_NOTICE("io_depth:%ld nr_event:%ld file_name:%s ctx:%lu\n", io_depth, max_nr_event,
                  aio_ctx->file_name, aio_ctx->ctx);
    tms.tv_sec = 1;
    tms.tv_nsec = 0;
    u_int64_t tsc_total_event = 0;
    u_int64_t tsc_total_aio_callback = 0;
    u_int64_t total_event_count = 0;
    u_int64_t tsc_event_begin = 0;
    u_int64_t tsc_event_end = 0;
    u_int64_t tsc_submit_end = 0;
    u_int64_t tsc_total_submit = 0;   /* total  tsc time for decrea_submit */
    u_int64_t total_submit_count = 0; /* count of decrea_submit */
    u_int64_t tsc_aio_callback_begin = 0;
    u_int64_t tsc_aio_callback_end = 0;
    u_int64_t total_callback = 0;
    u_int64_t last_offset = 0;
    u_int16_t last_op = IOCB_CMD_NOOP; /* only distinc read and write */
    while (1) {
        tsc_event_begin = get_tsc();
        int r = io_getevents(aio_ctx->ctx, 1, max_nr_event, events, &tms);
        if (r > 0) {
            submit_count = aio_ctx->submit_count;
            tsc_event_end = get_tsc();

            /* decrea io depth */
            decrea_submit(aio_ctx, r);

#ifdef PY_PROFILING
            tsc_total_event += (tsc_event_end - tsc_event_begin);
            tsc_submit_end = get_tsc();
            total_submit_count++;
            tsc_total_submit += tsc_submit_end - tsc_event_end;
#endif

            aio_ctx->event_count += r;

            /* handle evnet and callback */
            for (j = 0; j < r; j++) {
#ifdef PY_PROFILING
                total_event_count++;
                tsc_aio_callback_begin = get_tsc();
#endif /* PY_PROFILING */

                struct io_event *ioevent = events + j;
                struct user_iocb_list_t *user_iocb_list =(struct user_iocb_list_t *)ioevent->data;
                user_iocb_list->tsc_end = tsc_event_end;

                /* iocb has wait time, until it's front iocb complete */
                // if (user_iocb_list->tsc_start < tsc_event_begin) {
                //     user_iocb_list->tsc_start = tsc_event_begin;
                // }

                /* set io cost info, and judge io type */
                //u_int64_t cost_tm = (user_iocb_list->tsc_end - user_iocb_list->tsc_start) / r;
                if (aio_ctx->slow_disk_proerty.filter_io != NULL) {
                    aio_ctx->slow_disk_proerty.filter_io(aio_ctx, user_iocb_list, tsc_event_begin, r,
                                   submit_count, &last_op, &last_offset);
                }

                user_iocb_list->env = env;

                struct iocb *iocbp = &user_iocb_list->iocb;

                user_iocb_list->ioevent_res = ioevent->res;
                user_iocb_list->ioevent_res2 = ioevent->res2;

                /*  for callback thread*/
                poll_ret = task_queue_limit_offer(aio_ctx->tq_hdr, &user_iocb_list->list);
                if (poll_ret < 0) {
                    total_callback++;
                    PY_LOG_DEBUG(
                        "task queue:%s size:%u capacity:%u ret:%d, invock callback",
                        aio_ctx->tq_hdr->name, aio_ctx->tq_hdr->capacity,
                        aio_ctx->tq_hdr->size, poll_ret);
                    aio_done_callback(aio_ctx, user_iocb_list);
                }

#ifdef PY_PROFILING
                tsc_aio_callback_end = get_tsc();
                tsc_total_aio_callback +=
                    (tsc_aio_callback_end - tsc_aio_callback_begin);
                if ((total_event_count >= PROFILING_WINDOW) &&
                    (total_event_count % PROFILING_WINDOW == 0)) {
                    u_int64_t tsc_submit_once = tsc_submit_end - tsc_event_end;
                    u_int64_t tsc_aio_callback_once =
                        tsc_aio_callback_end - tsc_aio_callback_begin;
                    PY_LOG_NOTICE(
                        "storage:%s event count:%d event tsc:%lu  event "
                        "average:%lu "
                        "submit tsc:%lu. submit average:%lu aio callback "
                        "tsc:%lu "
                        "aio average:%lu total event count:0x%" PRIX64
                        " sub event count:0x%" PRIX64
                        " "
                        "submit count:0x%" PRIX64 "total callback:%lu",
                        aio_ctx->file_name, r,
                        (tsc_event_end - tsc_event_begin),
                        (tsc_total_event / total_event_count), tsc_submit_once,
                        (tsc_total_submit / total_submit_count),
                        tsc_aio_callback_once,
                        (tsc_total_aio_callback / total_event_count),
                        aio_ctx->event_count, total_event_count,
                        total_submit_count, total_callback);

                    tsc_total_event = 0;
                    tsc_total_aio_callback = 0;
                    total_event_count = 0;
                    total_submit_count = 0;
                    tsc_total_submit = 0;
                }
#endif /* PY_RPOFILING */
            }
		} else if(r == 0) {
			// timeout 
		    // TODO check expired 
            /* check exit thread condition */
            if(aio_ctx->stop_event_flag == 1) {
                if(aio_ctx->submit_count == 0) {
                    /* submit io had already been completed */
                    break;
                }
            }
            PY_LOG_INFO("file_name:%s time out\n", aio_ctx->file_name);
		} else {
			// error
            PY_LOG_ERROR("ret:%d file_name:%s", r, aio_ctx->file_name );
		}

	}
     
    free(events);
    (*jvm)->DetachCurrentThread(jvm);
    aio_ctx->finished_event_flag = 1;
    PY_LOG_NOTICE("event thread finised:%s", thread_name);
    return (void *)0;
    /// thread finished
}

static void datanode_aio_thread(long ctx_ptr) {
    pthread_t td_event, td_callback;
    struct datanode_aio_ctx_t *aio_ctx = (struct datanode_aio_ctx_t *)ctx_ptr;
    int ret =
        pthread_create(&td_callback, NULL, datanode_aio_callback_handler, (void *)aio_ctx);
    if (ret < 0) {
        PY_LOG_ERROR(
            "pthread create aio callback thread error. storage:%s ret:%d",
            aio_ctx->file_name, ret);
        return;
    }
    pthread_detach(td_callback);
    ret = pthread_create(&td_event, NULL, datanode_aio_event_handler, (void *)aio_ctx);
    if (ret < 0) {
        PY_LOG_ERROR("pthread create aio event thread error. storage:%s ret:%d",
                     aio_ctx->file_name, ret);
        return;
    }
    pthread_detach(td_event);
}

static void __async_io_free(struct datanode_aio_ctx_t *aio_ctx) {
    if (aio_ctx == NULL) {
        PY_LOG_ERROR("aio_ctx is null pointer");
        return;
    }

    PY_LOG_NOTICE("storage:%s resource be free", aio_ctx->file_name);
    datanode_io_pattern_destory(aio_ctx);
    datanode_aio_icobs_destory(aio_ctx);
    datanode_disk_checker_destory(aio_ctx);
    task_queue_destory(aio_ctx->tq_hdr);
    io_destroy(aio_ctx->ctx);
    close(aio_ctx->fd);
    free(aio_ctx);

    return;
}

JNIEXPORT void JNICALL Java_py_storage_async_AsyncFileAccessor_close(
    JNIEnv *env, jclass clazz, jlong ctx_ptr) {
    struct datanode_aio_ctx_t *aio_ctx = (struct datanode_aio_ctx_t *)ctx_ptr;
    if (aio_ctx == NULL) {
        PY_LOG_ERROR("close storage error. aio_ctx is null poniter");
        return;
    }

    PY_LOG_NOTICE("begin to close storage:%s", aio_ctx->file_name);

    aio_ctx->stop_event_flag = 1;
    int wait_time = 5;
    while (wait_time > 0) {
        if (aio_ctx->finished_event_flag == 1) {
            break;
        }
        sleep(1);

        wait_time--;
    }

    wait_time = 5;
    while (wait_time > 0) {
        if (aio_ctx->finished_callback_flag == 1) {
            break;
        }
        sleep(1);

        wait_time--;
    }

    PY_LOG_WARNING(
        "storage:%s write total:%lu read total:%lu event_count:%lu "
        "submit_count:%u\n",
        aio_ctx->file_name, aio_ctx->write_total, aio_ctx->read_total,
        aio_ctx->event_count, aio_ctx->submit_count);

    if (aio_ctx->finished_callback_flag == 1) {
        /* event handle thread finished(exit), so we can free resource */
        __async_io_free(aio_ctx);
        PY_LOG_NOTICE("storage:%s has been closed, resouce  be release",
                     aio_ctx->file_name);
    } else {
        /* over timeout, but event handle thread don't finished(exit) */
        PY_LOG_ERROR("storage:%s will been closed, but resouce don't be free. finished_event_flag:%d, finished_callback_flag:%d",
                     aio_ctx->file_name, aio_ctx->finished_event_flag, aio_ctx->finished_callback_flag);
    }

    return;
}

void __attribute__((constructor)) conver_cpu_hz() 
{
#if defined(__x86_64__) && defined(PY_RDTSC)
    if (cpu_hz_ns == 0) {
        u_int64_t begin_tsc = get_tsc();
        usleep(1000 * 1000);
        u_int64_t end_tsc = get_tsc();

        cpu_hz_ns = (end_tsc - begin_tsc) / TSC_NS_SECOND;
        if(cpu_hz_ns == 0) {
            PY_LOG_ERROR("-----ERRO. can't get cpu hz-------");
            exit(-1);
        } else {
            PY_LOG_NOTICE("cpu hz ns:%lu tsc:%lu", cpu_hz_ns, end_tsc - begin_tsc);
        }
    }
#else 
     PY_LOG_NOTICE("don't need compute cpu hz");
#endif
}
