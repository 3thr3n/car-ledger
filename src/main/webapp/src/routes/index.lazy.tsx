import CarGridList from '@/components/carGrid/CarGridList'
import { createLazyFileRoute } from '@tanstack/react-router'

export const Route = createLazyFileRoute('/')({
  component: Index,
})

function Index() {
  return (
    <CarGridList />
  )
}