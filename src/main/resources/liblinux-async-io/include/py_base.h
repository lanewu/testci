#ifndef __PY_BASE__H
#define __PY_BASE__H
#include <syslog.h>

#define PY_E_SUC   0
#define PY_E_SYS   1
#define PY_E_MEM   2
#define PY_EINVAL  3

#define PY_TQ_EFULL 4   /* task queue full */

#define PY_TRUE       1
#define PY_FALSE      0

#define PY_ENABLED     1
#define PY_DISABLED    0

#define PY_DEBUG_TAG      "DEBUG"
#define PY_INFO_TAG       "INFO"
#define PY_NOTICE_TAG     "NOTICE"
#define PY_WARNING_TAG    "WARNING"
#define PY_ERROR_TAG      "ERROR"
extern int py_debug_output ;// 0; syslog 1:console
extern int py_debug_level;
#define PY_DEBUG(p, t, a, x...)                                     \
    do                                                           \
    {                                                            \
        if (p <= py_debug_level)                                  \
        {                                                        \
            if(py_debug_output == 1) {                                                 \
                printf("[%s]%s:%d: async-io: " a, t, __func__, __LINE__, ##x);                         \
                printf("\n");                                                                          \
            } else {                                                                   \
                syslog(p, "[%s] %s:%s:%d: async-io: "a, t, __FILE__, __func__, __LINE__, ##x );        \
            }                                                                          \
        }                                                                              \
    } while (0)


#define PY_LOG_ERROR(x,...)             PY_DEBUG(LOG_ERR, PY_ERROR_TAG, x,  ##__VA_ARGS__ )
#define PY_LOG_WARNING(x,...)           PY_DEBUG(LOG_WARNING, PY_WARNING_TAG, x, ##__VA_ARGS__)
#define PY_LOG_NOTICE(x,...)            PY_DEBUG(LOG_NOTICE, PY_NOTICE_TAG, x, ##__VA_ARGS__ )
#define PY_LOG_INFO(x,...)              PY_DEBUG(LOG_INFO, PY_INFO_TAG, x, ##__VA_ARGS__ )
#define PY_LOG_DEBUG(x,...)             PY_DEBUG(LOG_DEBUG, PY_DEBUG_TAG, x, ##__VA_ARGS__ )

#endif /* __PY_BASE__H */