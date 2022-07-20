#include <stdlib.h>
#include <errno.h>

#include "py_base.h"
#include "py_list.h"
#include "task_queue.h"

#define POLL_LIMIT     0
#define POLL_UNLIMIT    1
struct task_queue_hdr *task_queue_init(u_int32_t capacity, char *name) {
    int ret;
    struct task_queue_hdr *tq_hdr = (struct task_queue_hdr *) calloc(1, sizeof(struct task_queue_hdr));
    if(tq_hdr == NULL) {
        PY_LOG_ERROR("calloc task_queue_hdr error.");
        return NULL;        
    }

    strncpy(tq_hdr->name, name, PY_QUEUE_NAME_SIZE - 1);
    tq_hdr->capacity = capacity;
    tq_hdr->size = 0;

    ret = pthread_mutex_init(&tq_hdr->queue_lock, NULL);
	if(ret != PY_E_SUC) {
        PY_LOG_ERROR("pthread_mutex_init  lock error. %d:%s", errno, strerror(errno));
		goto release_tq;
	}

    ret = pthread_cond_init(&tq_hdr->empty_wait, NULL);
	if(ret != PY_E_SUC) {
        PY_LOG_ERROR("pthread_cond_init submit wait error. %d:%s",  errno, strerror(errno));
		goto release_lock;
	}

	INIT_LIST_HEAD(&tq_hdr->task_list.list);

    PY_LOG_NOTICE("task queue(%s) init finished!. capacity:%u", tq_hdr->name, capacity);
    goto out;


release_lock:
    pthread_mutex_destroy(&tq_hdr->queue_lock);

release_tq:
    if(tq_hdr != NULL) {
        free(tq_hdr);
        tq_hdr = NULL;
    }
    PY_LOG_ERROR("task queue init error, release resource.");
out:
    return tq_hdr;
}

static int32_t task_queue_offer(struct task_queue_hdr *tq_hdr, /*struct task_node_list *node */ struct list_head *node, int limit  ) {

    int ret = PY_E_SUC;
    if(tq_hdr == NULL || node == NULL) {
        PY_LOG_ERROR("tq_hdr or list is null.");
        return -PY_E_SYS;
    }

    //PY_LOG_DEBUG("task poll node:%lu", *((u_int64_t *)(node->data)));
    // get lock
    if(pthread_mutex_lock(&tq_hdr->queue_lock) != 0) {
        PY_LOG_ERROR("lock error.");
        return -PY_E_SYS;
    }        

    if((limit == POLL_LIMIT) && (tq_hdr->capacity > 0)) {
        if (tq_hdr->size >= tq_hdr->capacity)
        {
            PY_LOG_INFO("queue is full. %s:%u.",tq_hdr->name, tq_hdr->size);
            ret = -PY_TQ_EFULL;
            goto out;
        }
    }

    list_add_tail(node, &tq_hdr->task_list.list);  
    tq_hdr->size ++;

out:
    pthread_cond_signal(&tq_hdr->empty_wait);

    pthread_mutex_unlock(&tq_hdr->queue_lock);

    return ret;
}

int32_t task_queue_unlimit_offer(struct task_queue_hdr *tq_hdr, struct list_head *node) {
    return task_queue_offer(tq_hdr, node, POLL_UNLIMIT);
}
int32_t task_queue_limit_offer(struct task_queue_hdr *tq_hdr, struct list_head *node) {
    return task_queue_offer(tq_hdr, node, POLL_LIMIT);
}

int32_t task_queue_take(struct task_queue_hdr *tq_hdr, struct task_node_list *nodes[], int count, u_int32_t abs_second) {

    struct task_node_list *node;
    struct list_head *n, *pos;
    int32_t size = 0;
    struct timespec ts;
    if(tq_hdr == NULL) {
        PY_LOG_ERROR("tq_hdr is null.");
        return -1;
    }

    // get lock
    if(pthread_mutex_lock(&tq_hdr->queue_lock) != 0) {
        PY_LOG_ERROR("lock error.");
        return -1;
    }

    PY_LOG_DEBUG("tq_hdr:0x%p", tq_hdr);
    // check queue size
    if(tq_hdr->size == 0) {
        clock_gettime(CLOCK_REALTIME, &ts);
        //ts.tv_nsec = 0;
        ts.tv_sec += abs_second;
        int ret = pthread_cond_timedwait(&tq_hdr->empty_wait, &tq_hdr->queue_lock, &ts);
        if(ret == ETIMEDOUT) {
            PY_LOG_INFO("---NOTICE----- wait over one second. queue size:%u. storage:%s", tq_hdr->size, tq_hdr->name);
            size = 0;
            goto out;
        }
    }    

    // take task from queue
    while(tq_hdr->size > 0) {
        list_for_each_safe(pos, n, &tq_hdr->task_list.list) {
            node = list_entry(pos, struct task_node_list, list);
            if(node != NULL) {
                tq_hdr->size --;
                PY_LOG_DEBUG("node data:%lu", *(u_int64_t *)node->data);
                nodes[size++] = node;
                list_del(&node->list);
            }

            // get enough tasks
            if(size >= count) {
                goto out;
            }
        }         
    }

out:
    // unlock
    pthread_mutex_unlock(&tq_hdr->queue_lock);
    return size;
}

void task_queue_destory(struct task_queue_hdr *tq_hdr) {

    PY_LOG_NOTICE("task queue(%s) destory.", tq_hdr->name);
    pthread_mutex_destroy(&tq_hdr->queue_lock);
    pthread_cond_destroy(&tq_hdr->empty_wait);

    free(tq_hdr);
}
