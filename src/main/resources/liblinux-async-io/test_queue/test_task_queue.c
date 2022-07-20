
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <sys/time.h>
#include <assert.h>

#include "py_base.h"
#include "task_queue.h"

struct task_queue_hdr *queue_hdr;
struct my_task_node {
    struct list_head list;
    u_int64_t index;
    u_int32_t id;
};

static void *custom( void *arg ) {

    u_int64_t count = *(u_int64_t *)arg;

    PY_LOG_DEBUG("count:%lu", count);
    u_int64_t i = 0;
    for(i = 0; i < count;) {
        struct my_task_node *task_node = malloc(sizeof(struct my_task_node));
        //u_int64_t *index = malloc(sizeof(u_int64_t));
        //*index = i;
        task_node->index = i;    
        task_node->id = i;
        int ret = task_queue_limit_offer(queue_hdr, &task_node->list);
        if(ret != PY_E_SUC) {
            free(task_node);
        } else {
            i++;
        }
    }

    return (void *)0;
}

static void *product(void *arg) {
    PY_LOG_NOTICE("product thread start"); u_int64_t count = *(u_int64_t*)arg;
    int32_t ret, i;
    u_int64_t finush_count = 0;
    
    struct task_node_list *task_node_array[32];
    while(1) {
        ret = task_queue_take(queue_hdr, task_node_array, 32, 1 );
        if(ret < 0) {
            PY_LOG_ERROR("task queue error");
            return (void *)0;
        }

        if(ret > 0) {
            for(i = 0; i < ret; i++) {
                struct my_task_node *node = (struct my_task_node *) task_node_array[i];
                //u_int64_t index = (u_int64_t)(task_node_array[i]->data);
                PY_LOG_DEBUG("data:%lu", node->index);
                //free(task_node_array[i]->data);
                assert(node->index == node->id);
                free(node);
            }
            finush_count += ret;
        }

        if(finush_count == count) {
            PY_LOG_NOTICE("finished count:%lu", finush_count);
            break;
        }
    }

    return (void *)0;
}

static pthread_t  product_thread(u_int64_t total) {
    
    int ret = 0;
    pthread_t  td;
    u_int64_t *count = malloc(sizeof(u_int64_t));
    *count = total; 
     ret = pthread_create(&td, NULL, (void *)product, (void*)count);

    return td;
     //pthread_detach(td);
}

static void custom_thread(u_int64_t size) {
    int ret = 0;
    u_int64_t  *count = malloc(sizeof(u_int64_t));
    *count = size;
    pthread_t td;
    ret = pthread_create(&td, NULL, (void *)custom, (void *)count);
    pthread_detach(td);
}

int py_debug_output = 1;
int py_debug_level = LOG_NOTICE;

int test_performance(int c, uint32_t count) 
{
    struct timeval tv;
    gettimeofday(&tv, NULL);
    PY_LOG_NOTICE("start:sec:%lu usec:%lu", tv.tv_sec, tv.tv_usec);
    queue_hdr = task_queue_init(1024, "queu-test") ;
    if(queue_hdr == NULL) {
        PY_LOG_ERROR("queue init error");
        return -1;
    }

    PY_LOG_INFO("queue_hde:0x%p size:%u name:%s", queue_hdr, queue_hdr->size, queue_hdr->name);
    u_int64_t total = c * count;
    u_int64_t size = count;
    PY_LOG_NOTICE("custom:%d total:%ld", c, total);
    pthread_t td = product_thread(total);

    for(int i = 0; i < c; i++) { 
        custom_thread(size);
    }

    pthread_join(td, NULL);
    gettimeofday(&tv, NULL);
    PY_LOG_NOTICE("end:sec:%lu usec:%lu", tv.tv_sec, tv.tv_usec);
    task_queue_destory(queue_hdr);
    return 0;
}

int main(int argc, char *argv[]) {

   int ret;

    struct timeval tv;
    gettimeofday(&tv, NULL);
    PY_LOG_NOTICE("start:sec:%lu usec:%lu", tv.tv_sec, tv.tv_usec);
    queue_hdr = task_queue_init(1024, "queu-test") ;
    if(queue_hdr == NULL) {
        PY_LOG_ERROR("queue init error");
        return -1;
    }

    PY_LOG_INFO("queue_hde:0x%p size:%u name:%s", queue_hdr, queue_hdr->size, queue_hdr->name);
    u_int64_t total = 10000000;
    u_int64_t size =  2500000;
    pthread_t td = product_thread(total);
   
    custom_thread(size);
    custom_thread(size);
    custom_thread(size);
    custom_thread(size);

    pthread_join(td, NULL);
    gettimeofday(&tv, NULL);
    PY_LOG_NOTICE("end:sec:%lu usec:%lu", tv.tv_sec, tv.tv_usec);
    task_queue_destory(queue_hdr);
   if(ret =0)
   {
       //
   }

   test_performance(1, 10000000);
}