import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { taskService } from '../../services/taskService'
import { format } from 'date-fns'
import toast from 'react-hot-toast'
import { FiSend } from 'react-icons/fi'
import Spinner from '../common/Spinner'

const TaskComments = ({ taskId }) => {
  const queryClient = useQueryClient()
  const [newComment, setNewComment] = useState('')

  // Fetch comments — unwrap paginated or plain array response
  const { data: rawComments, isLoading } = useQuery({
    queryKey: ['task-comments', taskId],
    queryFn: () => taskService.getComments(taskId),
  })

  const comments = Array.isArray(rawComments)
    ? rawComments
    : rawComments?.content ?? rawComments?.data?.content ?? []

  // Add comment mutation
  const addCommentMutation = useMutation({
    mutationFn: (content) => taskService.addComment(taskId, { message: content }),
    onSuccess: () => {
      queryClient.invalidateQueries(['task-comments', taskId])
      setNewComment('')
      toast.success('Comment added')
    },
    onError: () => {
      toast.error('Failed to add comment')
    },
  })

  const handleSubmit = (e) => {
    e.preventDefault()
    if (newComment.trim()) {
      addCommentMutation.mutate(newComment)
    }
  }

  if (isLoading) {
    return (
      <div className="flex justify-center py-8">
        <Spinner />
      </div>
    )
  }

  return (
    <div className="space-y-4">
      {/* Comment Form */}
      <form onSubmit={handleSubmit} className="flex gap-2">
        <input
          type="text"
          value={newComment}
          onChange={(e) => setNewComment(e.target.value)}
          placeholder="Add a comment... (use @username to mention)"
          className="input flex-1"
          disabled={addCommentMutation.isPending}
        />
        <button
          type="submit"
          disabled={!newComment.trim() || addCommentMutation.isPending}
          className="btn-primary flex items-center gap-2"
        >
          {addCommentMutation.isPending ? (
            <Spinner size="sm" />
          ) : (
            <>
              <FiSend />
              <span>Send</span>
            </>
          )}
        </button>
      </form>

      {/* Comments List */}
      <div className="space-y-4">
        {comments.length === 0 ? (
          <p className="text-center text-gray-500 dark:text-gray-400 py-8">
            No comments yet. Be the first to comment!
          </p>
        ) : (
          comments.map((comment) => (
            <div
              key={comment.id}
              className="flex gap-3 p-4 bg-gray-50 dark:bg-gray-700/50 rounded-lg"
            >
              {/* Avatar */}
              <div className="w-8 h-8 rounded-full bg-primary-500 flex items-center justify-center text-white text-sm font-medium flex-shrink-0">
                {comment.user?.fullName?.charAt(0).toUpperCase()}
              </div>

              {/* Content */}
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2 mb-1">
                  <span className="font-medium text-sm">
                    {comment.user?.fullName}
                  </span>
                  <span className="text-xs text-gray-500 dark:text-gray-400">
                    {format(new Date(comment.createdAt), 'MMM dd, yyyy HH:mm')}
                  </span>
                </div>
                <p className="text-sm text-gray-700 dark:text-gray-300 whitespace-pre-wrap">
                  {comment.content}
                </p>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  )
}

export default TaskComments
